/*
 * ZFPReceiptInfo.java
 *
 */

package com.example.vdovin.tremolprint.protocol.tremol;

/** ZFPReceiptInfo interface is result of executing of {@link com.tremol.zfplibj.ZFPLib#getCurrentReceiptInfo} method. The values are valid only for the present opened receipt
*/

public class ZFPReceiptInfo {
    
    protected int m_purchases;
    protected float[] m_taxgrp;
    protected boolean m_noVoid;
    protected boolean m_VATIncl;
    protected boolean m_extended;
    protected boolean m_payStart;
    protected boolean m_payEnd;
    protected boolean m_powerDown;
    protected boolean m_invoice;
    protected float m_change;
    protected boolean m_openReceipt;

    /** Creates a new instance of ZFPReceiptInfo */
    public ZFPReceiptInfo(byte[] output, int outputLen, int lang) throws ZFPException {
        String data = new String(output, 6, outputLen - 10);

        m_openReceipt = (output[4] == 0x31) ? true : false;
        if (m_openReceipt) {
            String[] s = new String(output, 6, outputLen - 10).split(";");
            if (12 > s.length) 
                throw new ZFPException(0x106, lang);

            int groups = s.length - 9;
            m_taxgrp = new float[groups];

            m_purchases = Integer.parseInt(s[0]);
            for (int i = 1; i <= groups; i++)
                m_taxgrp[i - 1] = Float.parseFloat(s[i]);

            m_noVoid = s[groups + 1].charAt(0) == '1' ? true : false;
            m_VATIncl = s[groups + 2].charAt(0) == '1' ? true : false;
            m_extended = s[groups + 3].charAt(0) == '1' ? true : false;
            m_payStart = s[groups + 4].charAt(0) == '1' ? true : false;
            m_payEnd = s[groups + 5].charAt(0) == '1' ? true : false;
            m_powerDown = s[groups + 6].charAt(0) == '1' ? true : false;
            m_invoice = s[groups + 7].charAt(0) == '1' ? true : false;

            m_change = Float.parseFloat(s[groups + 8]);
        }
        else {
            m_change = Float.parseFloat(new String(output, 60, 11));
            
            m_taxgrp = new float[3];
            for (int i = 0; i < 3; i++)
                m_taxgrp[i] = 0.0f;
            
            m_purchases = 0;
            m_noVoid = m_VATIncl = m_extended = m_payStart = m_payEnd = m_powerDown = m_invoice = false;
        }
    }

    /** Gets the number of sales for current receipt
     *  @return number of items sold
     */
    public int getPurchaces() {
        return m_purchases;
    }
    
    /** Gets the sum accumulated in certain tax group
     *  @param index tax group number
     *  @return the tax sum accumulated to the tax group
     */
    public float getTaxGroup(int index) {
        if ((0 > index) || (m_taxgrp.length <= index))
            return 0;
        
        return m_taxgrp[index];
    }
    
    /** Gets void function permition status
     *  @return true if void is forbidden, false - void is allowed
     */
    
    public boolean getVoidStatus() {
        return m_noVoid;
    }

    /** Gets status for VAT print field presence 
     *  @return true if VAT field is included in the receipt print out, false - VAT is not included
     */
    
    public boolean isVATIncluded() {
        return m_VATIncl;
    }

    /** Gets status for extended receipt format
     *  @return true if the receipt is in extended format, false - the receipt is in normal format
     */
    public boolean isExtended() {
        return m_extended;
    }

    /** Gets information for initiated payment
     *  @return true if the payment process is started, false - no payment process yet
     */
    public boolean isPaymentStarted() {
        return m_payStart;
    }

    /** Gets information for finished payment
     *  @return true if the payment is completed, false - payment is not completed
     */
    
    public boolean isPaymentFinished() {
        return m_payEnd;
    }

    /** Gets information for power down occured
     *  @return true if there was power down, false - normal start
     */
    public boolean isPowerDown() {
        return m_powerDown;
    }
    
    /** Gets information whether the receipt is simple one or invoice
     *  @return true if the receipt is invoice, false - receipt is not an invoice
     */
    public boolean isInvoice() {
        return m_invoice;
    }

    /** Gets information about the state of receipt - if its opened or not
     *  @return true if the receipt is opened, false - no opened receipt
     */
    
    public boolean isOpenReceipt() {
        return m_openReceipt;
    }
    
    /** Gets the change if any
     *  @return change if any 
     */
    public float getChange() {
        return m_change;
    }
}
