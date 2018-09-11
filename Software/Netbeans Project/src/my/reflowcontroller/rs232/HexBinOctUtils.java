/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package my.my.reflowcontroller.rs232;

/**
 *
 * @author hendriksit
 */
public class HexBinOctUtils {
   public static String getHexString(byte[] b) {
        String result = "";

        for (int i = 0; i < b.length; i++) {
            // Format to hex
            result += String.format("%02X", b[i]) + " ";
            //result += Integer.toHexString(b[i]) + "  ";
        }
        return result;
    }

    public static String getOctString(byte[] b) {
        String result = "";

        for (int i = 0; i < b.length; i++) {
            // Format to octal

            result += String.format("%03o", b[i]) + " ";
//            String temp = Integer.toOctalString(b[i]);
//            for (int j = 0; (temp.length() + j) < 3; j++) {
//                result += "0";
//            }
//            result += Integer.toOctalString(b[i]) + "   ";
        }
        return result;
    }

    public static String getBinString(byte[] b) {
        String result = "";

        for (int i = 0; i < b.length; i++) {
            // Format to binary

            String temp = Integer.toBinaryString((b[i]&0xff));
            for (int j = 0; (temp.length() + j) < 8; j++) {
                result += "0";
            }
            result += Integer.toBinaryString((b[i]&0xff)) + "  ";
        }
        return result;
    }

    public static byte[] hexStringToByteArray(String s) {

        s = s.replaceAll("0x", "");
        s = s.replaceAll("0X", "");
        s = s.replaceAll("[^a-fA-F0-9]*", "");

        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    } 
}
