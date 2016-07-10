package pku.abe.web;

import pku.abe.data.model.*;

/**
 * Created by vontroy on 7/10/16.
 */
public interface ABE {
    KeyInfo[] setup();

    SecretKeyInfo keygen(KeyInfo publicKey, KeyInfo masterKey, AttributeInfo[] attributes );

    CiphertextInfo encrypt(KeyInfo publicKey, PolicyInfo policy, byte[] message, byte[] symmetricKey );

    byte[] decrypt(CiphertextInfo ciphertext, KeyInfo secretKey);
}
