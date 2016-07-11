package pku.abe.web;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pku.abe.commons.util.PairingManager;
import pku.abe.commons.util.Utils;
import pku.abe.data.model.AttributeInfo;
import pku.abe.data.model.CiphertextInfo;
import pku.abe.data.model.KeyInfo;
import pku.abe.data.model.PolicyInfo;
import pku.abe.data.model.SecretKeyInfo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Created by vontroy on 7/10/16.
 */
public class rw13 implements ABE {
    private Pairing pairing = PairingManager.getDefaultPairing();
    private Logger logger = LoggerFactory.getLogger(rw13.class);

    @Override
    public KeyInfo[] setup() {
        Element alpha = pairing.getZr().newRandomElement().getImmutable();

        KeyInfo masterKey = new KeyInfo(KeyInfo.Type.MASTER);
        masterKey.getComponents().put("alpha", alpha);

        // Generate public parameters
        Element g1 = pairing.getG1().newRandomElement().getImmutable();
        Element g2 = pairing.getG2().newRandomElement().getImmutable();
        Element u = pairing.getG1().newRandomElement().getImmutable();
        Element h = pairing.getG1().newRandomElement().getImmutable();
        Element w = pairing.getG1().newRandomElement().getImmutable();
        Element v = pairing.getG1().newRandomElement().getImmutable();
        Element e_gg_alpha = pairing.pairing(g1, g2).powZn(alpha).getImmutable();

        KeyInfo publicKey = new KeyInfo(KeyInfo.Type.PUBLIC);
        publicKey.getComponents().put("g1", g1);
        publicKey.getComponents().put("g2", g2);
        publicKey.getComponents().put("u", u);
        publicKey.getComponents().put("h", h);
        publicKey.getComponents().put("w", w);
        publicKey.getComponents().put("v", v);
        publicKey.getComponents().put("e_gg_alpha", e_gg_alpha);

        // set the key array
        KeyInfo[] res = new KeyInfo[2];
        res[0] = publicKey;
        res[1] = masterKey;

        return res;
    }

    @Override
    public SecretKeyInfo keygen(KeyInfo publicKey, KeyInfo masterKey, AttributeInfo[] attributes) {
        if (publicKey.getType() != KeyInfo.Type.PUBLIC) {
            logger.error("require public key!");
        } else if (masterKey.getType() != KeyInfo.Type.MASTER) {
            logger.error("require master key!");
        }

        if (attributes == null || attributes.length == 0) return null;

        Element g1, g2, w, h, u, v;
        g1 = publicKey.getComponents().get("h1").duplicate().getImmutable();
        g2 = publicKey.getComponents().get("g2").duplicate().getImmutable();
        w = publicKey.getComponents().get("w").duplicate().getImmutable();
        h = publicKey.getComponents().get("h").duplicate().getImmutable();
        u = publicKey.getComponents().get("u").duplicate().getImmutable();
        v = publicKey.getComponents().get("v").duplicate().getImmutable();

        // obtain master key
        Element alpha = masterKey.getComponents().get("alpha").duplicate().getImmutable();

        // generate r
        Element r = pairing.getZr().newRandomElement().getImmutable();

        SecretKeyInfo sk = new SecretKeyInfo();

        // set the attributes associated with secret key
        sk.setAttributes(attributes);

        // compute K0, K1
        Element K0 = g1.powZn(alpha).mul(w.powZn(r)).getImmutable();
        Element K1 = g2.powZn(r).getImmutable();
        sk.getComponents().put("K0", K0);
        sk.getComponents().put("K1", K1);

        // init hash function
        MessageDigest sha256_hash = null;

        try {
            sha256_hash = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < attributes.length; i++) {
            // generate random r tau
            Element rtau = pairing.getZr().newRandomElement().getImmutable();

            // compute Ktau2
            Element Ktau2 = g2.powZn(rtau).getImmutable();

            // hash the attribute to a fixed length
            sha256_hash.update(attributes[i].toString().getBytes());
            byte[] fixedlengthattr = sha256_hash.digest();

            // obtain A tau
            Element Atau = pairing.getZr().newElementFromBytes(fixedlengthattr).getImmutable();

            // compute Ktau3
            Element Ktau3 = (u.powZn(Atau).mul(h)).powZn(rtau).mul(v.powZn(r.negate())).getImmutable();
            sk.getComponents().put("K" + attributes[i] + "2", Ktau2);
            sk.getComponents().put("K" + attributes[i] + "3", Ktau3);
        }
        return sk;
    }

    @Override
    public CiphertextInfo encrypt(KeyInfo publicKey, PolicyInfo policy, byte[] message, byte[] symmetricKey) {
        if (publicKey.getType() != KeyInfo.Type.PUBLIC) {
            logger.error("require public key!");
            return null;
        }

        // obtain the public parameters
        Element w = publicKey.getComponents().get("w").duplicate().getImmutable();
        Element v = publicKey.getComponents().get("v").duplicate().getImmutable();
        Element u = publicKey.getComponents().get("u").duplicate().getImmutable();
        Element h = publicKey.getComponents().get("h").duplicate().getImmutable();
        Element g2 = publicKey.getComponents().get("g2").duplicate().getImmutable();

        CiphertextInfo ciphertext = new CiphertextInfo();

        // compute LSSS corresponding to policy
        int[][] matrix = policy.getMatrix();
        Map<Integer, String> rho = policy.getRho();
        if (matrix == null) return null;

        // set LSSS
        ciphertext.setMatrix(matrix);
        ciphertext.setRho(rho);

        // generate s
        Element s = pairing.getZr().newRandomElement().getImmutable();
        // System.out.println("s:"+s.toString());

        // generate vector y.
        Element[] y = new Element[matrix[0].length];
        y[0] = s;
        for (int i = 1; i < y.length; i++) {
            y[i] = pairing.getZr().newRandomElement().getImmutable();
        }

        // compute shares
        Element[] shares = Utils.multiple(matrix, y);
        // System.out.println("shares:");
        // Utils.printArray(shares);

        // //for testing
        // this.shares=shares;

        // set the policy associated with ciphertext
        ciphertext.setPolicy(policy);

        // compute m
        Element m = pairing.getGT().newElementFromHash(symmetricKey, 0, symmetricKey.length);
        System.out.println("m:" + m);


        // SymmetricEncryption se = new SymmetricEncryption();
        // //encrypt message
        // try {
        // byte[] messageCihpertext=se.aesEncryptToBytes(message ,m.toBytes());
        // ciphertext.setLoad(messageCihpertext);
        // } catch (Exception e1) {
        // // TODO Auto-generated catch block
        // e1.printStackTrace();
        // }


        // compute C
        Element e_gg_alpha = publicKey.getComponents().get("e_gg_alpha").duplicate();
        Element C = m.mul(e_gg_alpha.powZn(s)).getImmutable();
        ciphertext.getComponents().put("C", C);

        // compute C0
        Element C0 = g2.powZn(s);
        ciphertext.getComponents().put("C0", C0);

        // init hash funcion
        MessageDigest sha256_hash = null;

        try {
            sha256_hash = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        for (int i = 0; i < matrix.length; i++) {

            Element ttau = pairing.getZr().newRandomElement().getImmutable();
            String attr = rho.get(i);

            // hash the attribute to a fixed length.
            sha256_hash.update(attr.getBytes());
            byte[] fixedlengthattr = sha256_hash.digest();

            // compute C_tau_1,C_tau_2
            Element C_tau_1 = w.powZn(shares[i]).mul(v.powZn(ttau)).getImmutable();
            Element C_tau_2 = (u.powZn(pairing.getZr().newElementFromBytes(fixedlengthattr)).mul(h)).powZn(ttau.negate()).getImmutable();
            Element C_tau_3 = g2.powZn(ttau).getImmutable();
            ciphertext.getComponents().put("C" + attr + "1", C_tau_1);
            ciphertext.getComponents().put("C" + attr + "2", C_tau_2);
            ciphertext.getComponents().put("C" + attr + "3", C_tau_3);
        }
        // System.out.println(ciphertext);
        return ciphertext;
    }

    @Override
    public byte[] decrypt(CiphertextInfo ciphertext, KeyInfo secretKey) {

        if (secretKey.getType() != KeyInfo.Type.SECRET) {
            logger.error("require secret key!");
        }

        SecretKeyInfo sk = (SecretKeyInfo) secretKey;

        // obtain the LSSS from ciphertext
        int[][] matrix = ciphertext.getMatrix();
        Map<Integer, String> rho = ciphertext.getRho();


        // get the users' attributes from secretkey
        AttributeInfo[] attributes = sk.getAttributes();

        // attribute matching
        Map<Integer, Integer> setI = AttributeInfo.attributesMatching(attributes, rho);

        // compute omega corresponding to set I
        Element[] omega = Utils.computeOmega(matrix, setI);
        if (omega == null) {
            return null;
        }

        // //print the recovery value for testing
        // Element[] shares = new Element[setI.size()];
        // Iterator<Integer> keySetIterator = setI.keySet().iterator();
        // for(int i=0;keySetIterator.hasNext();i++){
        // shares[i] = this.shares[keySetIterator.next()];
        //
        // }
        // System.out.println("innerProduct:" + Utils.innerProduct(shares, omega));

        AttributeInfo[] attrsetI = new AttributeInfo[setI.size()];
        int j = 0;
        for (Map.Entry<Integer, Integer> entry : setI.entrySet()) {

            if (rho.get(entry.getKey()).equals(attributes[entry.getValue()].toString())) {
                attrsetI[j] = attributes[entry.getValue()];
                j++;
            } else
                logger.error("SetI Error!");
        }

        Element C0 = ciphertext.getComponents().get("C0").duplicate().getImmutable();
        Element K0 = sk.getComponents().get("K0").duplicate().getImmutable();
        Element numerator = pairing.pairing(C0, K0).getImmutable();

        Element denominator = pairing.getGT().newOneElement().getImmutable();

        for (int i = 0; i < attrsetI.length; i++) {

            Element Ci1 = ciphertext.getComponents().get("C" + attrsetI[i] + "1").duplicate().getImmutable();
            Element K1 = sk.getComponents().get("K1").duplicate().getImmutable();

            Element Ci2 = ciphertext.getComponents().get("C" + attrsetI[i] + "2").duplicate().getImmutable();
            Element Ktau2 = sk.getComponents().get("K" + attrsetI[i] + "2").duplicate().getImmutable();

            Element Ci3 = ciphertext.getComponents().get("C" + attrsetI[i] + "3").duplicate().getImmutable();
            Element Ktau3 = sk.getComponents().get("K" + attrsetI[i] + "3").duplicate().getImmutable();

            Element t = (pairing.pairing(Ci1, K1).mul(pairing.pairing(Ci2, Ktau2)).mul(pairing.pairing(Ci3, Ktau3))).powZn(omega[i])
                    .getImmutable();

            denominator = denominator.mul(t).getImmutable();

        }
        // compute B
        Element B = numerator.div(denominator).getImmutable();
        // obtain C
        Element C = ciphertext.getComponents().get("C").duplicate();

        Element res = C.div(B).getImmutable();
        System.out.println("res:" + res.toString());

        // byte[] messageCleartext=null;
        // SymmetricEncryption se =new SymmetricEncryption();
        // decrypt message
        // try {
        // messageCleartext=se.aesDecryptByBytes(ciphertext.getLoad(), res.toBytes());
        // } catch (Exception e1) {
        // // TODO Auto-generated catch block
        // e1.printStackTrace();
        // }
        //// System.out.println("");
        return res.toBytes();
    }

    public static void main(String[] args) throws Exception{

        rw13 scheme = new rw13();
        KeyInfo[] publicKey = scheme.setup();

        System.out.println( publicKey[0].toString() );
        System.out.println( publicKey[1].toString() );


//        rw13 scheme = new rw13();
//        KeyInfo[] keys = scheme.setup();
//
//        AttributeInfo[] attributes = new AttributeInfo[2];
//        attributes[0] = new AttributeInfo("school", "pku");
//        attributes[1] = new AttributeInfo("academy", "computer");
//        KeyInfo sk = scheme.keygen(keys[0], keys[1], attributes);
//
////		String s = "school:pku and (academy:software or academy:computer)";
//        String s = "(school:pku and academy:computer) or (school:mit and academy:software)";
////		String s = "school:pku and academy:computer";
////		String s1 = "(school:pku and academy:software) or (school:mit and academy:computer)";
////		 s1 = "school:mit or academy:software";
//        PolicyInfo policy = new PolicyInfo(s);
//        byte[] symmetricKey = "this is the symmetric key bytes".getBytes();
//        CiphertextInfo ciphertext = scheme.encrypt(keys[0], policy, "it is a joke".getBytes("utf-8"),
//                symmetricKey);
//
//        byte[] messageCleartext = scheme.decrypt(ciphertext, sk);
//        String strMessageCleartext=new String(messageCleartext,"utf-8");
//
//        System.out.println(strMessageCleartext);
    }

}
