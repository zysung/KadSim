package peersim;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.UUID;

public  class SHA1 {
    public static String shaEncode(String inStr) throws Exception {
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance("SHA");
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
            return "";
        }

        byte[] byteArray = inStr.getBytes("UTF-8");
        byte[] md5Bytes = sha.digest(byteArray);
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16) {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }

    public static void main(String args[]) throws Exception {
        String str = UUID.randomUUID().toString();
        System.out.println("原始：" + str);
        System.out.println("SHA后：" + shaEncode(str));
        BigInteger bigInteger = new BigInteger(shaEncode(str),16);
        System.out.println(bigInteger);
        BigInteger bn3 = new BigInteger("2");
        BigInteger bn4 = new BigInteger("1");

        for (int i= 0; i < 160; i++) {
            bn4=bn4.multiply(bn3);
        }
        System.out.println(bn4);
        System.out.println(bigInteger.compareTo(bn4));
    }
}
