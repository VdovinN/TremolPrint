/*
 * ZFPOperatorInfo.java
 *
 */

package com.example.vdovin.tremolprint.protocol.tremol;

/**
  * ZFPOperatorInfo interace is the result of {@link com.tremol.zfplibj.ZFPLib#getOperatorInfo} method
  * @author <a href="http://tremol.bg/">Tremol Ltd.</a> (Stanimir Jordanov)
  */

public class ZFPOperatorInfo {
    
    protected int m_oper;
    protected String m_name;
    protected String m_pass;
    
    /** Creates a new instance of ZFPOperatorInfo */
    public ZFPOperatorInfo(int oper, byte[] output, int outputLen, int lang) throws ZFPException
    {
        m_oper = oper;
        try {
            m_name = new String(output, 6, 20).trim();
            m_pass = new String(output, 27, 4);
        } catch (Exception e) {
            throw new ZFPException(0x106, lang);
        }
    }
    
    /** Gets the operator number 
     *  @return operator number 
     */
    public int getNumber()
    {
        return m_oper;
    }

    /** Gets the operator name 
     *  @return operator name 
     */
    public String getName()
    {
        return m_name;
    }

    /** Gets the operator password 
     *  @return operator password 
     */
    
    public String getPassword()
    {
        return m_pass;
    }
}
