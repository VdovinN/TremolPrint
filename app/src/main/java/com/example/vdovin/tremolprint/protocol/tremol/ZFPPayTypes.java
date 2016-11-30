/*
 * ZFPPayTypes.java
 *
 */

package com.example.vdovin.tremolprint.protocol.tremol;

 /** ZFPPayTypes inteface is the result of {@link com.tremol.zfplibj.ZFPLib#getPayTypes} method
  * @author <a href="http://tremol.bg/">Tremol Ltd.</a> (Stanimir Jordanov)
  */

public class ZFPPayTypes {
    
    protected String[] m_types;
    
    /** Creates a new instance of ZFPPayTypes */
    public ZFPPayTypes(byte[] output, int outputLen, int lang) throws ZFPException 
    {
        m_types = new String[5];
        try {
            m_types[0] = new String(output, 4, 10).trim();
            m_types[1] = new String(output, 15, 10).trim();
            m_types[2] = new String(output, 26, 10).trim();
            m_types[3] = new String(output, 37, 10).trim();
            m_types[4] = new String(output, 48, 10).trim();
        } catch (Exception e) {
            throw new ZFPException(0x106, lang);
        }
    }
 
    /** Gets the name of certain type of payment
     *  @param index number of payment type
     *  @return name of payment type
     */
    public String getPayType(int index)
    {
        return m_types[index];
    }
    
    /** Gets all names of payment types
     *  @return array containing the names of all payment types
     */
    public String[] getPayTypes()
    {
        return m_types;
    }
    
    
}
