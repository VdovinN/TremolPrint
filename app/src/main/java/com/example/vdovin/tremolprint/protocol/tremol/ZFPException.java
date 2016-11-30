/*
 * ZFPException.java
 *
 * Created on ���������, 2005, ��������� 8, 11:01
 *
 */

package com.example.vdovin.tremolprint.protocol.tremol;

import com.example.vdovin.tremolprint.protocol.sun.PrintfFormat;

/**
 *
 * @author <a href="http://tremol.bg/">Tremol Ltd.</a> (Stanimir Jordanov)
 */
public class ZFPException extends Exception {
    
    /** Error text will be in Bulgarian
     *  @see ZFPLib#setLanguage(int)
     */
    public static final int ZFP_LANG_BG = 1;
    /** Error text will be in English
     *  @see ZFPLib#setLanguage(int)
     */
    public static final int ZFP_LANG_EN = 0;

    /**
     * Creates a new instance of <code>ZFPException</code> without detail message.
     */
    public ZFPException() {
    }
    
    
    /**
     * Constructs an instance of <code>ZFPException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public ZFPException(String msg) {
        super(msg);
    }
    
    public ZFPException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs an instance of <code>ZFPException</code> with the specified error code and lanuage id.
     * @param error the error code
     * @param lang langiage id
     * @see ZFPException#ZFP_LANG_BG
     */
    public ZFPException(int error, int lang) {
        super(getErrorString(error, lang));
    }
    
    /**
     * Return text representation of the error with given error code and language id
     * @param error the error code
     * @param lang langiage id
     * @return text representation of the error
     * @see ZFPException#ZFP_LANG_BG
     */
    static public String getErrorString(int error, int lang)
    {
        switch (lang) {
        case ZFP_LANG_BG: 
            return getErrorStringBG(error);
        }
        return getErrorStringEN(error);
    }
    
    static protected String getErrorStringBG(int error)
    {
        StringBuffer data = new StringBuffer(error);
        data.append(" (");
        data.append(new PrintfFormat("%04x").sprintf(error));
        data.append(" hex) ");
        if (0 == error) {
            data.append("���������� ������� �������!");
    	}
        else if (0x100 < error) {
            switch (error) {
            case 0x101:
                data.append("���������� ������ �����!");
                break;

            case 0x102:
                data.append("���������� ����� �� ������� �� ��������� �������!");
                break;

            case 0x103:
                data.append("����������� (NACK) ������� �� ��������� �������!");
                break;

            case 0x104:
                data.append("������ ��������� ����");
                break;

            case 0x105:
                data.append("������� � ������ ������� �� ��������� ������� / ������ ��� �����������!");
                break;

            case 0x106:
                data.append("�������� �� ������ ����� �� ��������� ������� / ������ ��� �����������!");
                break;

            case 0x107:
                data.append("��������� ������� �� ���� �� ������� ���������� / �������� ��-�����!");
                break;

            case 0x108:
                data.append("��������� ���� �� ����!");
                break;

            case 0x109:
                data.append("��������� ������� �� ���� �� ���� ������!");
                break;

            case 0x10A:
                data.append("��������� ������� � ���� / �������� ��-�����!");
                break;

            case 0x10B:
                data.append("�������� ����� �� ���� �� ������� / ������ ��� �����������!");
                break;

            case 0x10C:
                data.append("������ / ������� ������!");
                break;

            case 0x10D:
                data.append("��������� ������� � ���� (���������� ����� �� �������)!");
                break;

            case 0x10E:
                data.append("��������� ����������!");
                break;

            default:
                data.append("��������� ������!");
                break;
            } // switch
        }
        else {
            int err = error >> 4;
            switch (err) {
            case 0:
                data.append("�.�.: ��");
                break;

            case 1:
                data.append("�.�.: ���� ������");
                break;

            case 2:
                data.append("�.�.: ���������� � ������ ��������");
                break;

            case 3:
                data.append("�.�.: �������� / ������ ��������");
                break;

            case 4:
                data.append("�.�.: ������� �������� ���");
                break;

            case 5:
                data.append("�.�.: ������ � ������� �� ������� (������� ���)");
                break;

            case 6:
                data.append("�.�.: ������� ���������� ���");
                break;

            case 7:
                data.append("�.�.: ������ � ���������� ������� (������� ���)");
                break;

            case 8:
                data.append("�.�.: �������� ����� ���� �� ������");
                break;

            case 9:
                data.append("�.�.: ������ ������ ��� ����������� �������");
                break;

            case 0x0A:
                data.append("�.�.: ������� ������ �������");
                break;

            case 0x0B:
                data.append("�.�.: 24 ���� ��� ������ ����� (����������)");
                break;

            case 0x0C:
                data.append("�.�.: ������� �������");
                break;

            case 0x0D:
                data.append("�.�.: ���� �� ���������� ��� �������� ���");
                break;

            default:
                data.append( "�.�.: ��������� ������");
                break;
            } // switch

            data.append(" / ");
            err = error & 0x0F;
            switch (err) {
            case 0:
                data.append("�������: ��!");
                break;

            case 1:
                data.append("�������: ���������!");
                break;

            case 2:
                data.append("�������: �����������!");
                break;

            case 3:
                data.append("�������: ����������� ������ ������� �����!");
                break;

            case 4:
                data.append("�������: ����������� ������!");
                break;

            case 5:
                data.append("�������: ����������� ������ / ���������� �� �������� ��������!");
                break;

            case 6:
                data.append("�������: ����������� ������ / ����� ������ ��������!");
                break;

            case 7:
                data.append("�������: ������ ���������� ����� �� �� �������!");
                break;

            case 8:
                data.append("�������: ������������ ������� ����!");
                break;

            default:
                data.append("�������: ��������� ������!");
                break;
            } // switch
        } // else

        return data.toString();
    }

    static protected String getErrorStringEN(int error)
    {
        StringBuffer data = new StringBuffer(error);
        data.append(" (");
        data.append(new PrintfFormat("%04x").sprintf(error));
        data.append(" hex) ");
        if (0 == error) {
            data.append("operation completed successfully!");
    	}
        else if (0x100 < error) {
            switch (error) {
            case 0x101:
                data.append("incorrect input data!");
                break;

            case 0x102:
                data.append("timeout while waiting for fiscal printer response!");
                break;

            case 0x103:
                data.append("fiscal printer returned negative (NACK) response!");
                break;

            case 0x104:
                data.append("CRC error");
                break;

            case 0x105:
                data.append("bad fiscal printer response / communication error!");
                break;

            case 0x106:
                data.append("bad fiscal printer response data / communication error!");
                break;

            case 0x107:
                data.append("fiscal printer cannot complete the operation / try again later!");
                break;

            case 0x108:
                data.append("invalid logo file!");
                break;

            case 0x109:
                data.append("fiscal printer device cannot be found!");
                break;

            case 0x10A:
                data.append("fiscal printer is busy / try again later!");
                break;

            case 0x10B:
                data.append("bad data block number / communication error!");
                break;

            case 0x10C:
                data.append("file I/O error!");
                break;

            case 0x10D:
                data.append("fiscal printer is busy (timeout while waiting)!");
                break;

            case 0x10E:
                data.append("invalid device found!");
                break;

            default:
                data.append("unknown error!");
                break;
            } // switch
        }
        else {
            int err = error >> 4;
            switch (err) {
            case 0:
                data.append("F.P.: ��");
                break;

            case 1:
                data.append("F.P.: paper out");
                break;

            case 2:
                data.append("F.P.: daily registers overflow or 24h report blocked");
                break;

            case 3:
                data.append("F.P.: not adjusted/wrong clock");
                break;

            case 4:
                data.append("F.P.: opened fiscal receipt");
                break;

            case 5:
                data.append("F.P.: account with remainder to be paid (opened receipt)");
                break;

            case 6:
                data.append("F.P.: opened non fiscal receipt");
                break;

            case 7:
                data.append("F.P.: account payment finished (opened receipt)");
                break;

            case 8:
                data.append("F.P.: fiscal memory is read-only");
                break;

            case 9:
                data.append("F.P.: bad password or command not allowed");
                break;

            case 0x0A:
                data.append("F.P.: missing external display");
                break;

            case 0x0B:
                data.append("F.P.: 24 hours without daily report blocked");
                break;

            case 0x0C:
                data.append("F.P.: printer overheat");
                break;

            case 0x0D:
                data.append("F.P.: power down");
                break;

            default:
                data.append("F.P.: unknown error");
                break;
            } // switch

            data.append(" / ");
            err = error & 0x0F;
            switch (err) {
            case 0:
                data.append("command: ��!");
                break;

            case 1:
                data.append("command: invalid!");
                break;

            case 2:
                data.append("command: illegal!");
                break;

            case 3:
                data.append("command: denied because of uncommited report!");
                break;

            case 4:
                data.append("command: syntax error!");
                break;

            case 5:
                data.append("command: syntax error / input register overflow!");
                break;

            case 6:
                data.append("command: syntax error / input register is zero!");
                break;

            case 7:
                data.append("command: missing transaction for void!");
                break;

            case 8:
                data.append("command: insufficient subtotal!");
                break;

            default:
                data.append("command: unknown error!");
                break;
            } // switch
        } // else

        return data.toString();
    }
}
