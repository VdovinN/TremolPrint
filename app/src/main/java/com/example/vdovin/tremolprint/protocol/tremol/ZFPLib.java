/*
 * zfplib.java
 *
 */

package com.example.vdovin.tremolprint.protocol.tremol;

import com.example.vdovin.tremolprint.protocol.sun.PrintfFormat;

import java.io.*;
import java.util.*;
        
/**
 * ZFPLib is the main class responsible for communication with Zeka FP 
 * fiscal printer device. In order to be used with serial port, it requires 
 * <a href="http://java.sun.com/products/javacomm/">Java(tm) Communications API</a>
 * <p>Sample:</p>
 * <pre>
 * ZFPLib zfp = new ZFPLib(2, 9600); // COM2 baud rate 9600
 * zfp.openFiscalBon(1, "0000", false, false);
 * zfp.sellFree("Test article", '1', 2.34f, 1.0f, 0.0f);
 * zfp.sellFree("��������", '1', 1.0f, 3.54f, 0.0f);
 * float sum = zfp.calcIntermediateSum(false, false, false, 0.0f, '0');
 * zfp.payment(sum, 0, false);
 * zfp.closeFiscalBon();
 * </pre>
 *
 * @author <a href="http://tremol.bg/">Tremol Ltd.</a> (Stanimir Jordanov)
 * @version 1.0
 */
public class ZFPLib 
{
    /// constants
    
    /** Text is left aligned 
     *  @see #printText(String, int)
     */
    public static final int ZFP_TEXTALIGNLEFT = 0; 
    /** Text is right aligned
     *  @see #printText(String, int)
     */
    public static final int ZFP_TEXTALIGNRIGHT = 1;
    /** Text is centered
     *  @see #printText(String, int)
     */
    public static final int ZFP_TEXTALIGNCENTER = 2;
    
    protected final long g_timeout = 3000;
    protected final long p_timeout = 1000;

    protected OutputStream outputStream;
    protected InputStream inputStream;
    protected int m_lastNbl;
    protected byte[] m_receiveBuf;
    protected int m_receiveLen;
    protected int m_lang;

    public ZFPLib(InputStream inputStream, OutputStream outputStream){
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        init();
    }
    
    
    public void setLanguage(int language) {
        m_lang = language;
    }
    
    /** Return error message language
     *  @return language error message language 
     *  @see ZFPException#ZFP_LANG_EN
     */
    public int getLanguage() {
        return m_lang;
    }
    
    protected void init()
    {
        m_lastNbl = 0x20;
        m_receiveBuf = new byte[256];
        m_lang = ZFPException.ZFP_LANG_EN; // default is English
    }
    
    protected boolean makeCRC(byte[] data, int len, int mode)
    {
        // calculate the CRC
        byte crc = 0;
        for (int i = 1; i < len - 3; i++)
            crc ^= data[i];

        switch (mode) {
            case 0:
                // add the CRC
                data[len - 3] = (byte)((crc >> 4) | 0x30);
                data[len - 2] = (byte)((crc & 0x0F) | 0x30);
                break;
                
            case 1:
                // add the CRC
                byte test = (byte)(((crc >> 4) & 0x0F) | 0x30);
                test |= (byte)0x30;
                if (data[len - 3] != (byte)(((crc >> 4) & 0x0F) | 0x30))
                    return false;
                if (data[len - 2] != (byte)((crc & 0x0F) | 0x30))
                    return false;
                break;
        }
        return true;
    }
    
    protected boolean doPing(byte ping, int retries) throws ZFPException
    {
        byte[] b = new byte[1];
        for (int i = 0; i < retries; i++) {
            try {
                b[0] = (byte)0x03;  // antiecho
                outputStream.write(b);

                b[0] = ping;        // ping
                outputStream.write(b);

                b[0] = 0;
                long start = System.currentTimeMillis();
                do {
                    if (0 < inputStream.available()) {
                        inputStream.read(b);
                        if (b[0] == (byte)0x03) {
                            throw new ZFPException(0x10E, m_lang);
                        }
                        if (b[0] == ping) {
                            return true;
                        }
                        try {
                            wait(20);
                        }
                        catch (Exception e) {}
                    }
                } while (p_timeout > System.currentTimeMillis() - start);
            }
            catch (Exception e) {
                throw new ZFPException(e);
            }
        }
        throw new ZFPException(0x102, m_lang);
    }
    
    protected boolean checkForZFP() throws ZFPException
    {
        return doPing((byte)0x04, 10);
    }
    
    protected boolean checkForZFPBusy() throws ZFPException
    {
        return doPing((byte)0x05, 10);
    }

    protected void getResponse() throws ZFPException
    {
        int read;
        
        long start = System.currentTimeMillis();
        
        do {
            try {
                if (0 < inputStream.available()) {
                    read = inputStream.read(m_receiveBuf, 0, 1);
                    if (0 < read) {
                        if ((byte)0x06 == m_receiveBuf[0]) {  // ACK
                            break;
                        }
                        else if ((byte)0x02 == m_receiveBuf[0]) { // STX
                            break;
                        }
                        else if ((byte)0x15 == m_receiveBuf[0]) { // NACK
                            throw new ZFPException(0x103, m_lang);
                        }
                        else if ((byte)0x03 == m_receiveBuf[0]) { // ANTIECHO
                            throw new ZFPException(0x10E, m_lang);
                        }
                        else if ((byte)0x0E == m_receiveBuf[0]) { // RETRY
                            // ToDo
                            break;
                        }
                    }
                }
            }
            catch (Exception e) {
                throw new ZFPException(e);
            }
            
            if (g_timeout < System.currentTimeMillis() - start) {
                throw new ZFPException(0x102, m_lang);
            }
            
            try {
                if (0 == inputStream.available())
                    wait(20);
            }
            catch (Exception e) {}
        } while (true);
        
        // read the data
        m_receiveLen = 1;
        int avail;
        do {
            try {
                avail = inputStream.available();
            }
            catch (Exception e) {
                throw new ZFPException(e);
            }
            if (0 < avail) {
                try {
                    read = inputStream.read(m_receiveBuf, m_receiveLen, 1);
                }
                catch (Exception e) {
                    throw new ZFPException(e);
                }
                if (0 < read) {
                    if ((byte)0x0A == m_receiveBuf[m_receiveLen]) {
                        m_receiveLen += read;
                        break;
                    }
                    m_receiveLen += read;
                }
            }
            // timeout check
            if (g_timeout < System.currentTimeMillis() - start) {
                throw new ZFPException(0x102, m_lang);
            }
            try {
                wait(20);
            }
            catch (Exception e) {}
        } while (true);
        
        
        if (!makeCRC(m_receiveBuf, m_receiveLen, 1)) {
            throw new ZFPException(0x104, m_lang);
        }
        
        if ((byte)0x06 == m_receiveBuf[0]) {  // ACK
            if (((byte)0x30 != m_receiveBuf[2]) || ((byte)0x30 != m_receiveBuf[3])) {
                int error = Integer.parseInt(new String(m_receiveBuf, 2, 2), 16);
                throw new ZFPException(error, m_lang);
            }
        }
        else if (m_receiveBuf[2] != (byte)m_lastNbl) {
            throw new ZFPException(0x10B, m_lang);
        }
    }
    
    protected void sendCommand(byte cmd, byte[] data) throws ZFPException
    {
        checkForZFP();
        checkForZFPBusy();
        
        // prepare the command
        int len = (null != data) ? data.length : 0;
        byte[] fullCmd = new byte[4 + len + 3];
        fullCmd[0] = (byte)0x02;                // STX
        fullCmd[1] = (byte)(len + 0x20 + 0x03); // LEN
        if (0xFF < ++m_lastNbl)
            m_lastNbl = 0x20;
        fullCmd[2] = (byte)m_lastNbl;           // NBL
        fullCmd[3] = cmd;                       // CMD
        
        if (null != data)
            System.arraycopy(data, 0, fullCmd, 4, len);
        
        makeCRC(fullCmd, fullCmd.length, 0);
        fullCmd[fullCmd.length - 1] = (byte)0x0A; // ETX

        try {
            outputStream.write(fullCmd);
        }
        catch (Exception e) {
            throw new ZFPException(e);
        }
       
        getResponse();
    }

    static public String nstrcpy(String s, int maxlen)
    {
        if (maxlen < s.length())
            return s.substring(0, maxlen);
        return s;
    }

    static public String getFloatFormat(float num, int count)
    {
        float max_value = (2 == count) ? 9999999.99f : 999999.999f;
        String match;
        if (max_value < num) 
            match = "%.0f";
        else {
            match = "%010.";
            match += Integer.toString(count);
            match += "f";
        }

        String res = new PrintfFormat(match).sprintf(num).replace(',', '.');
        if ('.' == res.charAt(9))
            return res.substring(0, 9);

        return res;
    }

//////////////////////////////////////////////////////////////////////
// Commands
//////////////////////////////////////////////////////////////////////
    /** Gets Zeka FP status (errors and other flags)
     *  @return ZFPStatus class containing the current status
     *  @see ZFPStatus
     *  @exception ZFPException in case of communication error
     */
    public ZFPStatus getStatus() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x20, null);
        } finally {
             
        }
        return new ZFPStatus(m_receiveBuf, m_receiveLen, m_lang);
    }

    /** Runs Zeka FP diagnostic print
     *  @exception ZFPException in case of communication error
     */
    public void diagnostic() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x22, null);
        } finally {
             
        }
    }

    /** Gets Zeka FP firmware version info
     *  @return Zeka FP firmware version info
     *  @exception ZFPException in case of communication error
     */
    public String getVersion() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x21, null);
        } finally {
             
        }
        return new String(m_receiveBuf, 4, m_receiveLen - 7).trim();
    }
    /** Clears Zeka FP external display
     *  @exception ZFPException in case of communication error
     */
    public void displayClear() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x24, null);
        } finally {
             
        }
    }
    /** Displays text on the first line of the external display
     *  @param line string to be displayed, truncated to 20 characters when longer
     *  @exception ZFPException in case of communication error
     */
    public void displayLine1(String line) throws ZFPException
    {
        String data = new PrintfFormat("%-20s").sprintf(nstrcpy(line, 20));
         
        try {
            sendCommand((byte)0x25, data.getBytes());
        } finally {
             
        }
    }

    /** Displays text on the second line of the external display
     *  @param line string to be displayed, truncated to 20 characters when longer
     *  @exception ZFPException in case of communication error
     */
    public void displayLine2(String line) throws ZFPException
    {
        String data = new PrintfFormat("%-20s").sprintf(nstrcpy(line, 20));
         
        try {
            sendCommand((byte)0x26, data.getBytes());
        } finally {
             
        }
    }

     /** Displays text on both lines of the external display
     *  @param line string to be displayed, truncated to 40 characters when longer
     *  @exception ZFPException in case of communication error
     */
    public void display(String line) throws ZFPException
    {
        String data = new PrintfFormat("%-40s").sprintf(nstrcpy(line, 40));
         
        try {
            sendCommand((byte)0x27, data.getBytes());
        } finally {
             
        }
    }

     /** Displays current date and time on the external display
      *  @exception ZFPException in case of communication error
      */
    public void displayDateTime() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x28, null);
        } finally {
             
        }
    }

    /** Cuts the paper
     *  @exception ZFPException in case of communication error
     */
    public void paperCut() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x29, null);
        } finally {
             
        }
    }

    /** Opens the safe box
     *  @exception ZFPException in case of communication error
     */
    public void openTill() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x2A, null);
        } finally {
             
        }
    }

    /** Feeds one line of paper
     *  @exception ZFPException in case of communication error
     */
    public void lineFeed() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x2B, null);
        } finally {
             
        }
    }

    /** Gets Zeka FP manifacture number 
     *  @return manifacture number - string 10 characters long
     *  @exception ZFPException in case of communication error
     */
    public String getFactoryNumber() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x60, null);
        } finally {
             
        }
        return new String(m_receiveBuf, 4, 8).trim();
    }

    /** Gets Zeka FP Tax Memory number 
     *  @return Tax Memory number - string 10 characters long
     *  @exception ZFPException in case of communication error
     */
    public String getFiscalNumber() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x60, null);
        } finally {
             
        }
        return new String(m_receiveBuf, 13, 8).trim();
    }

    /** Gets Zeka FP Tax number 
     *  @return Tax number - string 14 characters long
     *  @exception ZFPException in case of communication error
     */
    public String getTaxNumber() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x61, null);
        } finally {
             
        }
        return new String(m_receiveBuf, 4, 13).trim();
    }

    /** Gets Zeka FP Tax Percents 
     *  @return ZFPTaxNumbers class - percentage of different tax groups
     *  @see ZFPTaxNumbers
     *  @exception ZFPException in case of communication error
     */
    public ZFPTaxNumbers getTaxPercents() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x62, null);
        } finally {
             
        }
        return new ZFPTaxNumbers(m_receiveBuf, m_receiveLen, m_lang, "%;");
    }

    /** Gets Zeka FP Decimal point position
     *  @return decimal point position 
     *  @exception ZFPException in case of communication error
     */
    public int getDecimalPoint() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x63, null);
        } finally {
             
        }
        return Integer.parseInt(new String(m_receiveBuf, 4, 1).trim());
    }

    /** Gets Zeka FP additional payment types names
     *  @return ZFPPayTypes class - names of additional payment types
     *  @see ZFPPayTypes
     *  @exception ZFPException in case of communication error
     */
    public ZFPPayTypes getPayTypes() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x64, null);
        } finally {
             
        }
        return new ZFPPayTypes(m_receiveBuf, m_receiveLen, m_lang);
    }

    /** Gets Zeka FP system parameter settings
     *  @return ZFPParameters class - parameter values
     *  @see ZFPParameters
     *  @exception ZFPException in case of communication error
     */
    public ZFPParameters getParameters() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x65, null);
        } finally {
             
        }
        return new ZFPParameters(m_receiveBuf, m_receiveLen, m_lang);
    }

    /** Gets Zeka FP system date and time
     *  @return java.util.Calendar class containing the date and time stored in Zeka FP
     *  @exception ZFPException in case of communication error
     */
    public Calendar getDateTime() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x68, null);
        } finally {
             
        }

        String[] s = new String(m_receiveBuf, 4, m_receiveLen - 7).split("[\\s-\\:]");
        if (5 != s.length) 
            throw new ZFPException(0x106, m_lang);

        Calendar cal = Calendar.getInstance();
        cal.set(Integer.parseInt(s[2]), Integer.parseInt(s[1]), Integer.parseInt(s[0]), Integer.parseInt(s[3]), Integer.parseInt(s[4]));
        return cal;
    }

    /** Gets Zeka FP Header and Footer lines
     *  @param line indicates the exact line to read (1 to 8)
     *  @return header or footer text - string up to 40 characters long
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public String getClisheLine(int line) throws ZFPException
    {
    	if ((8 < line) || (1 > line)) 
            throw new ZFPException(0x101, m_lang);
            
        String data = Integer.toString(line);
         
        try {
            sendCommand((byte)0x69, data.getBytes());
        } finally {
             
        }
        return new String(m_receiveBuf, 6, m_receiveLen - 10);
    }

    /** Gets Zeka FP Operator information
     *  @param oper indicates the exact number operator to read
     *  @return ZFPOperatorInfo class - information for the certain operator
     *  @see ZFPOperatorInfo
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public ZFPOperatorInfo getOperatorInfo(int oper) throws ZFPException
    {
    	if ((9 < oper) || (1 > oper)) 
            throw new ZFPException(0x101, m_lang);
            
        String data = Integer.toString(oper);
         
        try {
            sendCommand((byte)0x6A, data.getBytes());
        } finally {
             
        }
        return new ZFPOperatorInfo(oper, m_receiveBuf, m_receiveLen, m_lang);
    }

    /** Prints graphic logo 
     *  @exception ZFPException in case of communication error
     */
    public void printLogo() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x6C, null);
        } finally {
             
        }
    }

    /** Opens non client receipt
     *  @param oper indicates the exact number operator (1 to 9)
     *  @param pass string containing the certain operator password
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void openBon(int oper, String pass) throws ZFPException
    {
    	if ((9 < oper) || (1 > oper))
            throw new ZFPException(0x101, m_lang);

        String data = Integer.toString(oper);
        data += ";";
        data += new PrintfFormat("%-4s").sprintf(nstrcpy(pass, 4));
        
         
        try {
            sendCommand((byte)0x2E, data.getBytes());
        } finally {
             
        }
    }

    /** Closes the opened non client receipt
     *  @exception ZFPException in case of communication error
     */
    public void closeBon() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x2F, null);
        } finally {
             
        }
    }

    /** Opens  client receipt
     *  @param oper indicates the exact number operator (1 to 9) 
     *  @param pass string containing the certain operator password - 4 characters
     *  @param detailed flag for detailed or brief receipt (0 = brief, 1 = detailed)
     *  @param vat flag for printing the VAT tax sums separately (0 = do not print, 1 = print)
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void openFiscalBon(int oper, String pass, boolean detailed, boolean vat) throws ZFPException
    {
    	if ((9 < oper) || (1 > oper))
            throw new ZFPException(0x101, m_lang);

        StringBuffer data = new StringBuffer(Integer.toString(oper));
        data.append(";");
        data.append(new PrintfFormat("%-4s").sprintf(nstrcpy(pass, 4)));
        data.append(detailed ? ";1" : ";0");
        data.append(vat ? ";1;2" : ";0;2");
        
         
        try {
            sendCommand((byte)0x30, data.toString().getBytes());
        } finally {
             
        }
    }

    /** Opens  client invoice receipt
     *  @param oper indicates the exact number operator 
     *  @param pass string containing the certain operator password (1 to 9)
     *  @param client string containing client name - truncated to 26 characters when longer
     *  @param receiver string containing recipient name - truncated to 16 characters when longer
     *  @param taxnum string containing client tax number - truncated to 13 characters when longer
     *  @param bulstat string containing client bulstat number - truncated to 13 characters when longer
     *  @param address string containing client address - truncated to 30 characters when longer
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void openInvoice(int oper, String pass, String client, String receiver,
						 String taxnum, String bulstat, String address) throws ZFPException
    {
    	if ((9 < oper) || (1 > oper))
            throw new ZFPException(0x101, m_lang);

        StringBuffer data = new StringBuffer(Integer.toString(oper));
        data.append(";");
        data.append(new PrintfFormat("%-4s").sprintf(nstrcpy(pass, 4)));
        data.append(";0;0;1;");
        data.append(new PrintfFormat("%-26s").sprintf(nstrcpy(client, 26)));
        data.append(";");
        data.append(new PrintfFormat("%-16s").sprintf(nstrcpy(receiver, 16)));
        data.append(";");
        data.append(new PrintfFormat("%-13s").sprintf(nstrcpy(taxnum, 13)));
        data.append(";");
        data.append(new PrintfFormat("%-13s").sprintf(nstrcpy(bulstat, 13)));
        data.append(";");
        data.append(new PrintfFormat("%-30s").sprintf(nstrcpy(address, 30)));
        
         
        try {
            sendCommand((byte)0x30, data.toString().getBytes());
        } finally {
             
        }
    }

    /** Closes the opened client receipt
     *  @exception ZFPException in case of communication error
     */
    public void closeFiscalBon() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x38, null);
        } finally {
             
        }
    }

    /** Closes the opened client invoice receipt
     *  @exception ZFPException in case of communication error
     */
    public void closeInvoice() throws ZFPException
    {
        closeFiscalBon();
    }

    /** Registers item sell from PC database
     *  @param name string containing item description - truncated to 36 characters when longer
     *  @param taxgrp character characterizing the item tax group attachment (0, 1, 2, '0', '1', '2' for Bulgarian FP version)
     *  @param price item price
     *  @param quantity item quantity
     *  @param discount discount/addition in percents
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void sellFree(String name, char taxgrp, float price, float quantity, float discount) throws ZFPException
    {
    	if ((-99999999.0f > price) || (99999999.0f < price) || (0.0f > quantity) || 
        	(999999.999f < quantity) || (-999.0f > discount) || (999.0f < discount))
            throw new ZFPException(0x101, m_lang);

        StringBuffer data = new StringBuffer(new PrintfFormat("%-36s").sprintf(nstrcpy(name, 36)));
        data.append(";");
        data.append(taxgrp);
        data.append(";");
        data.append(getFloatFormat(price, 2));
        data.append("*");
        data.append(getFloatFormat(quantity, 3));
        if (0.0f != discount) {
            data.append(",");
            data.append(new PrintfFormat("%6.2f").sprintf(discount));
            data.append("%");
        }
        
         
        try {
            sendCommand((byte)0x31, data.toString().getBytes());
        } finally {
             
        }
    }

    /** Registers item sell from FP internal database
     *  @param isVoid flag specifing is it item sell or void 
     *  @param number item database number 
     *  @param quantity item quantity
     *  @param discount discount/addition in percents
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void sellDB(boolean isVoid, int number, float quantity, float discount) throws ZFPException
    {
    	if ((0 > quantity) || (9999999999.0f < quantity) || 
            (-999.0f > discount) || (999.0f < discount) || (0 > number))
            throw new ZFPException(0x101, m_lang);

        StringBuffer data = new StringBuffer();
        data.append(isVoid ? '-' : '+');
        data.append(";");
        data.append(new PrintfFormat("%05u").sprintf(number));
        data.append("*");
        data.append(getFloatFormat(quantity, 3));
        if (0.0f != discount) {
            data.append(",");
            data.append(new PrintfFormat("%6.2f").sprintf(discount));
            data.append("%");
        }
        
         
        try {
            sendCommand((byte)0x32, data.toString().getBytes());
        } finally {
             
        }
    }

    /** Calculates the sub total sum of the receipt
     *  @param print flag for print the sub total sum
     *  @param show flag for show the sub total sum on the external display
     *  @param isPercent flag for percentage discount/addition 
     *  @param discount discount/addition value
     *  @param taxgrp specifies the tax group - ignored in Bulgarian FP version
     *  @return returns the sub total sum
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public float calcIntermediateSum(boolean print, boolean show, boolean isPercent, 
            float discount, char taxgrp) throws ZFPException
    {
        StringBuffer data = new StringBuffer();
        data.append(print ? '1' : '0');
        data.append(";");
        data.append(show ? '1' : '0');
        if (0.0f != discount) {
            if (isPercent) {
                data.append(",");
                data.append(new PrintfFormat("%6.2f").sprintf(discount));
                data.append("%");
            }
            else {
                data.append(":");
                data.append(getFloatFormat(discount, 2));
            }
        }

         
        try {
            sendCommand((byte)0x33, data.toString().getBytes());
        } finally {
             
        }
        return Float.parseFloat(new String(m_receiveBuf, 4, m_receiveLen - 7).trim());
    }

    /** Registers payment of the receipt
     *  @param sum paid sum
     *  @param type specifies the payment type number (0 to 3)
     *  @param noRest specifies that no change is due to client when true (takes effect only with certain payment types)
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void payment(float sum, int type, boolean noRest) throws ZFPException
    {
    	if ((0 > type) || (4 < type) || (0.0f > sum) || (9999999999.0f < sum))
            throw new ZFPException(0x101, m_lang);

        String data = Integer.toString(type);
        data += noRest ? ";1;" : ";0;";
        data += getFloatFormat(sum, 2);
        
         
        try {
            sendCommand((byte)0x35, data.getBytes());
        } finally {
             
        }
    }
 
    /** Calcualtes the VAT of the receipt and transfers it in VAT Account
     *  @exception ZFPException in case of communication error
     */
    public void payVAT() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x36, null);
        } finally {
             
        }
    }

    /** Prints text
     *  @param text string containing the text to be printed - truncated to 34 characters when longer
     *  @param align text alignment
     *  @see #ZFP_TEXTALIGNLEFT
     *  @see #ZFP_TEXTALIGNRIGHT
     *  @see #ZFP_TEXTALIGNCENTER
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void printText(String text, int align) throws ZFPException
    {
        int newalign = align;
        if (34 <= text.length())
            newalign = ZFP_TEXTALIGNLEFT;

        String data;
        switch (newalign) {
        case ZFP_TEXTALIGNRIGHT:
            data = new PrintfFormat("%34s").sprintf(nstrcpy(text, 34));
            break;

        case ZFP_TEXTALIGNCENTER: {
                StringBuffer buf = new StringBuffer("                                  "); // 34 spaces
                int pos = (34 - text.length()) / 2;
                data = buf.replace(pos, pos + text.length(), text).toString();
            }
            break;

        default:
            data = new PrintfFormat("%-34s").sprintf(nstrcpy(text, 34));
            break;
        }

         
        try {
            sendCommand((byte)0x37, data.getBytes());
        } finally {
             
        }
    }

    /** Prints duplicate of the last client receipt
     *  @exception ZFPException in case of communication error
     */
    public void printDuplicate() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x3A, null);
        } finally {
             
        }
    }

    /** Registers official paid out and received on account sums
     *  @param oper indicates the exact number operator (1 to 9)
     *  @param pass string containing the certain operator password (4 characters)
     *  @param type specifies the payment type number (0 to 3)
     *  @param sum specifies the sum 
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void officialSums(int oper, String pass, int type, float sum) throws ZFPException
    {
    	if ((9 < oper) || (1 > oper) || (3 < type) || (0 > type) || (-999999999.0f > sum) || (9999999999.0f < sum))
            throw new ZFPException(0x101, m_lang);

        StringBuffer data = new StringBuffer(oper);
        data.append(";");
        data.append(new PrintfFormat("%-4s").sprintf(nstrcpy(pass, 4)));
        data.append(";");
        data.append(type);
        data.append(";");
        data.append(getFloatFormat(sum, 2));

         
        try {
            sendCommand((byte)0x3B, data.toString().getBytes());
        } finally {
             
        }
    }

    /** Gets item information from FP internal database
     *  @param number specifies the item database number (0 to 1000)
     *  @return ZFPArticle class - information of the specified item
     *  @see ZFPArticle
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public ZFPArticle getArticleInfo(int number) throws ZFPException
    {
    	if ((1000 < number) || (0 > number))
            throw new ZFPException(0x101, m_lang);
            
        String data = new PrintfFormat("%05d").sprintf(number);
         
        try {
            sendCommand((byte)0x6B, data.getBytes());
        } finally {
             
        }
        return new ZFPArticle(number, m_receiveBuf, m_receiveLen, m_lang);
    }

    /** Gets daily sums information for each tax group
     *  @return ZFPTaxNumbers class - sums for each tax group
     *  @see ZFPTaxNumbers
     *  @exception ZFPException in case of communication error
     */
    public ZFPTaxNumbers getDailySums() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x6D, null);
        } finally {
             
        }
        return new ZFPTaxNumbers(m_receiveBuf, m_receiveLen, m_lang, ";");
    }

    /** Gets the number of the last issued receipt
     *  @return number of the last issued receipt
     *  @exception ZFPException in case of communication error
     */
    public int getBonNumber() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x63, null);
            return Integer.parseInt(new String(m_receiveBuf, 4, m_receiveLen - 10).trim());
        } catch (Exception e) {
            throw new ZFPException(e);
        } finally {
             
        }
    }
    
//////////////////////////////////////////////////////////////////////
// Setup commands & tools
//////////////////////////////////////////////////////////////////////

    /** Sets the additional payment type names
     *  @param type payment type number (1 to 3)
     *  @param line payment type name - maximum 10 characters 
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void setPayType(int type, String line) throws ZFPException
    {
    	if ((1 > type) || (3 < type))
            throw new ZFPException(0x101, m_lang);

        String data = Integer.toString(type);
        data += ";";
        data += nstrcpy(line, 10);
        
         
        try {
            sendCommand((byte)0x44, data.getBytes());
        } finally {
             
        }
    }

    /** Sets Fiscal Printer general parameters
     *  @param fpnum Set the Zeka FP POS number (0 to 9999)
     *  @param logo logo printing status (false = not printed, else printed)
     *  @param till cash drawer status (false = no till, else till presence)
     *  @param autocut auto cutter status (false = don't cut receipt in the end automaticaly, else cut the receipt in the end automaticaly)
     *  @param transparent transparent external display status (false = not transparernt, else transparent)
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void setParameters(int fpnum, boolean logo, boolean till, 
            boolean autocut, boolean transparent) throws ZFPException
    {
    	if ((0 > fpnum) || (9999 < fpnum))
            throw new ZFPException(0x101, m_lang);

        StringBuffer data = new StringBuffer(new PrintfFormat("%04u").sprintf(fpnum));
        data.append(";");
        data.append(logo ? '1' : '0');
        data.append(";");
        data.append(till ? '1' : '0');
        data.append(";");
        data.append(autocut ? '1' : '0');
        data.append(";");
        data.append(transparent ? '1' : '0');
        
         
        try {
            sendCommand((byte)0x45, data.toString().getBytes());
        } finally {
             
        }
    }

    /** Sets the system date and time of Zeka FP
     *  @param cal Calendar class representing the time and date to be set
     *  @exception ZFPException in case of communication error
     */
    public void setDateTime(Calendar cal) throws ZFPException
    {
        StringBuffer data = new StringBuffer(new PrintfFormat("%02u").sprintf(cal.get(Calendar.DAY_OF_MONTH)));
        data.append("-");
        data.append(new PrintfFormat("%02u").sprintf(cal.get(Calendar.MONTH)));
        data.append("-");
        data.append(new PrintfFormat("%02u").sprintf(cal.get(Calendar.YEAR)));
        data.append(" ");
        data.append(new PrintfFormat("%02u").sprintf(cal.get(Calendar.HOUR_OF_DAY)));
        data.append(":");
        data.append(new PrintfFormat("%02u").sprintf(cal.get(Calendar.MINUTE)));
        data.append(":");
        data.append(new PrintfFormat("%02u").sprintf(cal.get(Calendar.SECOND)));
        
         
        try {
            sendCommand((byte)0x48, data.toString().getBytes());
        } finally {
             
        }
    }
    
    /** Sets Zeka FP Header and Footer lines
     *  @param line speciffies the exact line to set (1 - 8)
     *  @param text header or footer text - truncated to 38 characters when longer
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void setClicheLine(int line, String text) throws ZFPException
    {
    	if ((1 > line) || (8 < line))
            throw new ZFPException(0x101, m_lang);

        String data = Integer.toString(line);
        data += ";";
        data += nstrcpy(text, 38);
        
         
        try {
            sendCommand((byte)0x44, data.getBytes());
        } finally {
             
        }
    }

    /** Sets Zeka FP operators passwords
     *  @param oper speciffies the operator number (1 - 9)
     *  @param name string with desired name - truncated to 20 characters when longer
     *  @param pass string with desired password to set - truncated to 4 characters when longer
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void setOperatorUserPass(int oper, String name, String pass) throws ZFPException
    {
    	if ((1 > oper) || (9 < oper))
            throw new ZFPException(0x101, m_lang);

        StringBuffer data = new StringBuffer(Integer.toString(oper));
        data.append(";");
        data.append(new PrintfFormat("%-20s").sprintf(nstrcpy(pass, 20)));
        data.append(";");
        data.append(new PrintfFormat("%-4s").sprintf(nstrcpy(pass, 4)));
        
         
        try {
            sendCommand((byte)0x4A, data.toString().getBytes());
        } finally {
             
        }
    }

    /** Sets Zeka FP item from the internal database
     *  @param number the item number in the internal database (0 to 1000)
     *  @param name string with desired item name - truncated to 20 characters when longer
     *  @param price the item price  
     *  @param taxgrp item tax group attachment
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void setArticleInfo(int number, String name, float price, char taxgrp) throws ZFPException
    {
    	if ((0 > number) || (1000 < number) || (-999999999.0f > price) || (9999999999.0f < price))
            throw new ZFPException(0x101, m_lang);

        StringBuffer data = new StringBuffer(new PrintfFormat("%05u").sprintf(number));
        data.append(";");
        data.append(new PrintfFormat("%-20s").sprintf(nstrcpy(name, 20)));
        data.append(";");
        data.append(getFloatFormat(price, 2));
        data.append(";");
        data.append(taxgrp);
        
         
        try {
            sendCommand((byte)0x4B, data.toString().getBytes());
        } finally {
             
        }
    }

    /** Sets Zeka FP bitmap logo 
     *  @param filename name of the file to be uploaded (.BMP)
     *  @exception ZFPException if the input parameters are incorrect
     */
    public void setLogoFile(String filename) throws ZFPException
    {
        byte[] buf = new byte[3906];
		buf[0] = (byte)0x02;
		buf[1] = (byte)0x39;
		buf[2] = (byte)0x37;
		buf[3] = (byte)0x4C;

         
        try {
            FileInputStream fs = new FileInputStream(filename);
            if (3902 != fs.read(buf, 4, 3902)) {
                fs.close();
                throw new ZFPException(0x108, m_lang);
            }
            fs.close();
            outputStream.write(buf);
        } catch (Exception e) {
            new ZFPException(e);
        } finally {
             
        }
    }

    /** Sets Zeka FP system date and time based on the PC system clock
     *  @exception ZFPException in case of communication error
     */
    public void setLocalDateTime() throws ZFPException
    {
        setDateTime(Calendar.getInstance());
    }
    
//////////////////////////////////////////////////////////////////////
// Reports
//////////////////////////////////////////////////////////////////////

    /** Starts tax memory special report
     *  @exception ZFPException in case of communication error
     */
    public void reportSpecialFiscal() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x77, null);
        } finally {
             
        }
    }

    /** Starts tax memory report by block numbers
     *  @param detailed flag for brief or detailed report
     *  @param startNumber start block number (0 to 9999)
     *  @param endNumber end block number (0 to 9999)
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void reportFiscalByBlock(boolean detailed, int startNumber, int endNumber) throws ZFPException
    {
    	if ((0 > startNumber) || (9999 < startNumber) || (0 > endNumber) || (9999 < endNumber))
            throw new ZFPException(0x101, m_lang);

        StringBuffer data = new StringBuffer(new PrintfFormat("%04u").sprintf(startNumber));
        data.append(";");
        data.append(new PrintfFormat("%04u").sprintf(endNumber));

         
        try {
            sendCommand(detailed ? (byte)0x78 : (byte)0x79, data.toString().getBytes());
        } finally {
             
        }
    }

    /** Starts tax memory report by date
     *  @param detailed flag for brief or detailed report
     *  @param start start date
     *  @param end end date
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void reportFiscalByDate(boolean detailed, Calendar start, Calendar end) throws ZFPException
    {
        if (start.after(end))
            throw new ZFPException(0x101, m_lang);
        
        StringBuffer data = new StringBuffer(new PrintfFormat("%02u").sprintf(start.get(Calendar.DAY_OF_MONTH)));
        data.append(new PrintfFormat("%02u").sprintf(start.get(Calendar.MONTH)));
        data.append(new PrintfFormat("%02u").sprintf(start.get(Calendar.YEAR)));
        data.append(";");
        data.append(new PrintfFormat("%02u").sprintf(end.get(Calendar.DAY_OF_MONTH)));
        data.append(new PrintfFormat("%02u").sprintf(end.get(Calendar.MONTH)));
        data.append(new PrintfFormat("%02u").sprintf(end.get(Calendar.YEAR)));
        
         
        try {
            sendCommand(detailed ? (byte)0x7A : (byte)0x7B, data.toString().getBytes());
        } finally {
             
        }
    }

    /** Starts Daily report
     *  @param zero speciffies the report as Zero Daily or X daily ('Z' or 'X')
     *  @param extended speciffies the report as brief or detailed
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void reportDaily(boolean zero, boolean extended) throws ZFPException
    {
        String data = zero ? "Z" : "X";
         
        try {
            sendCommand(extended ? (byte)0x7F : (byte)0x7C, data.getBytes());
        } finally {
             
        }
    }

    /** Starts Operators report
     *  @param zero true speciffies the report as 'Z' (zero report), false 'X' (information report)
     *  @param oper speciffies the operator number (0 - 9; 0 is for all operators)
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void reportOperator(boolean zero, int oper) throws ZFPException
    {
        if ((0 > oper) || (9 < oper))
            throw new ZFPException(0x101, m_lang);
        
        String data = zero ? "Z" : "X";
        data += ";";
        data += Integer.toString(oper);
         
        try {
            sendCommand((byte)0x7D, data.getBytes());
        } finally {
             
        }
    }

    /** Starts Operators report
     *  @param zero - true speciffies the report as 'Z' (zero report) false 'X' (information report)
     *  @exception ZFPException in case of communication error
     */
    public void reportArticles(boolean zero) throws ZFPException
    {
        String data = zero ? "Z" : "X";
         
        try {
            sendCommand((byte)0x7E, data.getBytes());
        } finally {
             
        }
    }

//////////////////////////////////////////////////////////////////////
// Service
//////////////////////////////////////////////////////////////////////

    /** Programming of external display with direct data
     *  @param password programming password
     *  @param data data to be programmed (see manual for details)
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void setExternalDisplayData(String password, byte[] data) throws ZFPException
    {
        if (101 < data.length)
            throw new ZFPException(0x101, m_lang);

        byte[] buf = new byte[6 + data.length];
        String pass = new PrintfFormat("%-6s").sprintf(nstrcpy(password, 6));
        byte[] passBuf = pass.getBytes();
        
        System.arraycopy(passBuf, 0, buf, 0, 6);
        System.arraycopy(data, 0, buf, 6, data.length);
        
         
        try {
            sendCommand((byte)0x7E, buf);
        } finally {
             
        }
    }

    /** Programming of external display with external data file
     *  @param password programming password
     *  @param filename name of file to be send for programming of external display (see manual for details)
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void setExternalDisplayFile(String password, String filename) throws ZFPException
    {
        byte[] buf = new byte[101];

        try {
            FileInputStream fs = new FileInputStream(filename);
            int read = fs.read(buf, 0, 101);
            if (101 > read) {
                byte[] buf2 = new byte[read];
                System.arraycopy(buf, 0, buf2, 0, read);
                setExternalDisplayData(password, buf2);
            }
            else
                setExternalDisplayData(password, buf);
            fs.close();
        } catch (Exception e) {
            new ZFPException(e);
        }
    }
    
    /** Read the tax memory contents in external data file
     *  @param filename filename of target file where the tax memory records are stored 
     *  file format: <br><pre>
     *  [NBL][CMD][segment number];[record code];[record date];[status];[data]
     *  �.
     *  �..
     *  [NBL][CMD][segment number];[record code];[record date];[status];[data]
     *  [NBL][CMD][segment number];[@] - end of records
     *  record code / record type
     *  00 Manifacture record
     *  01 Put into operation
     *  04 Daily report
     *  05 RAM Reset
     *  06 Tax percents change
     *  07 Decimal point change
     *  </pre>
     *  @exception ZFPException if the input parameters are incorrect
     */
    public void readFiscalMemory(String filename) throws ZFPException
    {
        // ToDo
    }
    
    /** Gets the number of free tax memory  blocks
     *  @return the number of free tax memory blocks
     *  @exception ZFPException in case of communication error
     */
    public int getFreeFiscalSpace() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x74, null);
            return Integer.parseInt(new String(m_receiveBuf, 4, m_receiveLen - 10).trim());
        } catch (Exception e) {
            throw new ZFPException(e);
        } finally {
             
        }
    }
    
    /** Sets manifacture and tax memory numbers of the device
     *  @param password manifacture password - 6 characters
     *  @param manifactureNum manifacture number - 6 characters
     *  @param fiscalNum tax memory number - 6 characters
     *  @param controlSum check sum 
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void setSerialNumber(String password, String manifactureNum, 
							 String fiscalNum, String controlSum) throws ZFPException
    {
        StringBuffer data = new StringBuffer(new PrintfFormat("%-6s").sprintf(nstrcpy(password, 6)));
        data.append(";");
        data.append(new PrintfFormat("%-6s").sprintf(nstrcpy(manifactureNum, 6)));
        data.append(";");
        data.append(new PrintfFormat("%-6s").sprintf(nstrcpy(fiscalNum, 6)));
        data.append(";");
        data.append(new PrintfFormat("%-6s").sprintf(nstrcpy(controlSum, 6)));

         
        try {
            sendCommand((byte)0x40, data.toString().getBytes());
        } finally {
             
        }
    }

    /** Sets tax number and tax memory numbers of the device
     *  @param password manifacture password - 6 characters
     *  @param taxNum tax number - 15 characters 
     *  @param fiscalNum tax memory number - 12 characters
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void setTaxNumber(String password, String taxNum, String fiscalNum) throws ZFPException
    {
        StringBuffer data = new StringBuffer(new PrintfFormat("%-6s").sprintf(nstrcpy(password, 6)));
        data.append(";1;");
        data.append(new PrintfFormat("%-15s").sprintf(nstrcpy(taxNum, 15)));
        data.append(";");
        data.append(new PrintfFormat("%-12s").sprintf(nstrcpy(fiscalNum, 12)));

         
        try {
            sendCommand((byte)0x41, data.toString().getBytes());
        } finally {
             
        }
    }

    /** Puts the device into operation and activates the tax memory
     *  @param password service password - 6 characters
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void makeFiscal(String password) throws ZFPException
    {
        String data = new PrintfFormat("%-6s").sprintf(nstrcpy(password, 6));
        data += ";2";
        
         
        try {
            sendCommand((byte)0x41, data.getBytes());
        } finally {
             
        }
    }

    /** Sets the tax percents
     *  @param password service password - 6 characters
     *  @param tgr1 tax group 1 percentage
     *  @param tgr2 tax group 2 percentage
     *  @param tgr3 tax group 3 percentage
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void setTaxPercents(String password, float tgr1, float tgr2, float tgr3) throws ZFPException
    {
    	if ((100.0f < tgr1) || (100.0f < tgr2) || (100.0f < tgr3)) 
            throw new ZFPException(0x101, m_lang);

        StringBuffer data = new StringBuffer(new PrintfFormat("%-6s").sprintf(nstrcpy(password, 6)));
        data.append(";");
        data.append(new PrintfFormat("%.2f").sprintf(tgr1));
        data.append("%;");
        data.append(new PrintfFormat("%.2f").sprintf(tgr2));
        data.append("%;");
        data.append(new PrintfFormat("%.2f").sprintf(tgr3));
        data.append("%");

         
        try {
            sendCommand((byte)0x42, data.toString().getBytes());
        } finally {
             
        }
    }
    
    /** Sets the decimal point position
     *  @param password service password - 6 characters
     *  @param point the decimal point position (0 or 2)
     *  @exception ZFPException if the input parameters are incorrect or in case of communication error
     */
    public void setDecimalPoint(String password, int point) throws ZFPException
    {
    	if ((0 > point) || (9 < point))
            throw new ZFPException(0x101, m_lang);
        
        String data = new PrintfFormat("%-6s").sprintf(nstrcpy(password, 6));
        data += ";";
        data += Integer.toString(point);

         
        try {
            sendCommand((byte)0x43, data.toString().getBytes());
        } finally {
             
        }
    }

    /** Gets information about current opened receipt
     *  @return ZFPReceiptInfo class information about the current receipt
     *  @exception ZFPException in case of communication error
     */
    public ZFPReceiptInfo getCurrentReceiptInfo() throws ZFPException
    {
         
        try {
            sendCommand((byte)0x72, null);
        } finally {
             
        }
        return new ZFPReceiptInfo(m_receiveBuf, m_receiveLen, m_lang);
    }
}
