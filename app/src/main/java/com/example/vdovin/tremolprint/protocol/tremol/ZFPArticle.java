/*
 * ZFPArticle.java
 *
 */

package com.example.vdovin.tremolprint.protocol.tremol;

import java.util.Calendar;

/**
  * ZFPArticle - interface is internal db article data and it is
  * result of {@link com.tremol.zfplibj.ZFPLib#getArticleInfo} method
  * @author <a href="http://tremol.bg/">Tremol Ltd.</a> (Stanimir Jordanov)
  */
public class ZFPArticle {
    
    protected int m_num;
    protected String m_name;
    protected float m_price;
    protected char m_taxgrp;
    protected float m_turnover;
    protected float m_sales;
    protected int m_counter;
    protected Calendar m_datetime;
    
    /** Creates a new instance of ZFPArticle */
    public ZFPArticle(int number, byte[] output, int outputLen, int lang) throws ZFPException 
    {
        m_num = number;

        m_name = new String(output, 10, 20).trim();
        String[] s = new String(output, 31, outputLen - 52).split("[;]"); // \\s-\\:
        if (5 != s.length) 
            throw new ZFPException(0x106, lang);

        String[] dat = new String(output, 75, 16).split("[\\s-\\:]");
        if (5 != s.length) 
            throw new ZFPException(0x106, lang);

        m_datetime = Calendar.getInstance();
        m_datetime.set(Integer.parseInt(dat[2]), Integer.parseInt(dat[1]), Integer.parseInt(dat[0]), Integer.parseInt(dat[3]), Integer.parseInt(dat[4]));

        try {
            m_price = Float.parseFloat(s[0].trim());
            m_taxgrp = s[1].charAt(0);
            m_turnover = Float.parseFloat(s[2].trim());
            m_sales = Float.parseFloat(s[3].trim());
            m_counter = Integer.parseInt(s[4].trim());
        } catch (Exception e) {
            throw new ZFPException(0x106, lang);
        }
    }

    /** Gets the number of an item
     *  @return item number 
     */
    public int getNumber()
    {
        return m_num;
    }
    
    /** Gets the name of an item
     *  @return item name
     */
    public String getName()
    {
        return m_name;
    }
    
    /** Gets the price of an item
     *  @return item price
     */
    public float getPrice()
    {
        return m_price;
    }
    
    /** Gets the tax group attachment of an item
     *  @return item tax group
     */
    public char getTaxGroup()
    {
        return m_taxgrp;
    }

    /** Gets the turnover accumulated by item sales
     *  @return item turnover 
     */
    public float getTurnover()
    {
        return m_turnover;
    }
    
    /** Gets the number of item sales 
     *  @return item sales number 
     */
    public float getSales()
    {
        return m_sales;
    }

    /** Gets the number of last item report
     *  @return number of last report
     */
    
    public int getReportCounter()
    {
        return m_counter;
    }
 
    /** Gets the date and time of last item report
     *  @return date and time of last report
     */
    
    public Calendar getReportDateTime()
    {
        return m_datetime;
    }
}
