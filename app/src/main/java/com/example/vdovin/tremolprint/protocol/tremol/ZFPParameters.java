/*
 * ZFPParameters.java
 *
 */

package com.example.vdovin.tremolprint.protocol.tremol;

/** ZFPParameters inteface is the result of {@link com.tremol.zfplibj.ZFPLib#getParameters} method
  * @author <a href="http://tremol.bg/">Tremol Ltd.</a> (Stanimir Jordanov)
  */
public class ZFPParameters {
    
    protected int m_fpNum;
    protected boolean m_logo;
    protected boolean m_till;
    protected boolean m_autocut;
    protected boolean m_transparent;
    
    /** Creates a new instance of ZFPParameters */
    public ZFPParameters(byte[] output, int outputLen, int lang) throws ZFPException 
    {
        String[] s = new String(output, 4, outputLen - 7).split(";");
        if (5 != s.length) 
            throw new ZFPException(0x106, lang);
        
        try {
            m_fpNum = Integer.parseInt(s[0]);
            m_logo = (0 != Integer.parseInt(s[1])) ? true : false;
            m_till = (0 != Integer.parseInt(s[2])) ? true : false;
            m_autocut = (0 != Integer.parseInt(s[3])) ? true : false;
            m_transparent = (0 != Integer.parseInt(s[4])) ? true : false;
        } catch (Exception e) {
            throw new ZFPException(0x106, lang);
        }
    }
    
    /** Gets the number the fiscal printer device
     *  @return number of device
     */
    public int getFpNum()
    {
        return m_fpNum;
    }
    
    /** Shows whether graphic logo is printed or not
     *  @return 0 for not printed, otherwise is printed
     */
    public boolean isLogoPrinted()
    {
        return m_logo;
    }

    /** Shows whether fiscal printer has cash drawer attached or not
     *  @return 0 for not attached, otherwise - attached
     */
    public boolean hasTill()
    {
        return m_till;
    }
    
    /** Shows whether the cutter is controled by the fical printer device (automatic) or by external methods
     *  @return 0 for not automatic, otherwise - automatic
     */

    public boolean isAutoCutting()
    {
        return m_autocut;
    }
    
    /** Shows whether the display is controled by the fiscal printer device (not transparent) or not
     *  @return 0 for not transparent, otherwise - controled by the fiscal printer device
     */

    public boolean isTransparentDisplay()
    {
        return m_transparent;
    }
}
