package pku.abe.api.lkme.web.sdk;

import pku.abe.data.model.AttributeInfo;
import pku.abe.data.model.CiphertextInfo;
import pku.abe.data.model.KeyInfo;
import pku.abe.data.model.PolicyInfo;

/**
 * Created by vontroy on 7/10/16.
 */
public class main {
    public static void main(String[] args) throws Exception{

        rw13 scheme = new rw13();
        KeyInfo[] keys = scheme.setup();

        AttributeInfo[] attributes = new AttributeInfo[2];
        attributes[0] = new AttributeInfo("school", "pku");
        attributes[1] = new AttributeInfo("academy", "computer");
        KeyInfo sk = scheme.keygen(keys[0], keys[1], attributes);

//		String s = "school:pku and (academy:software or academy:computer)";
        String s = "(school:pku and academy:computer) or (school:mit and academy:software)";
//		String s = "school:pku and academy:computer";
//		String s1 = "(school:pku and academy:software) or (school:mit and academy:computer)";
//		 s1 = "school:mit or academy:software";
        PolicyInfo policy = new PolicyInfo(s);
        byte[] symmetricKey = "this is the symmetric key bytes".getBytes();
        CiphertextInfo ciphertext = scheme.encrypt(keys[0], policy, "it is a joke".getBytes("utf-8"),
                symmetricKey);

        byte[] messageCleartext = scheme.decrypt(ciphertext, sk);
        String strMessageCleartext=new String(messageCleartext,"utf-8");

        System.out.println(strMessageCleartext);
    }
}
