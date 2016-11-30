/*
 * ZFPTaxNumbers.java
 *
 */

package com.example.vdovin.tremolprint.protocol.tremol;

 /** ZFPTaxNumbers inteface is the result of {@link com.tremol.zfplibj.ZFPLib#getTaxPercents} method
  * @author <a href="http://tremol.bg/">Tremol Ltd.</a> (Stanimir Jordanov)
  */

public class ZFPTaxNumbers {

    protected float[] m_taxgrp;
    
    /** Creates a new instance of ZFPTaxNumbers */
    public ZFPTaxNumbers(byte[] output, int outputLen, int lang, String delimiter) throws ZFPException 
    {
        String[] s = new String(output, 4, outputLen - 7).split(delimiter);
        m_taxgrp = new float[s.length];
        try {
            for (int i = 0; i < s.length; i++)
                m_taxgrp[i] = Float.parseFloat(s[i]);
        } catch (Exception e) {
            throw new ZFPException(0x106, lang);
        }
    }
    
    /** Gets the percent rate of certain type tax group
     *  @param index number tax group
     *  @return percent rate of the selected tax group
     */
    public float getTaxGrp(int index)
    {
        return m_taxgrp[index];
    }
    
    /** Gets percent rates of all tax groups
     *  @return array with percent rates of all tax groups
     */
    public float[] getTaxGroups()
    {
        return m_taxgrp;
    }
    
}
