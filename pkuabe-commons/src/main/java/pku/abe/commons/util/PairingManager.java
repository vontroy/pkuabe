package pku.abe.commons.util;

import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.parameters.PropertiesParameters;

import java.io.InputStream;

/**
 * Created by vontroy on 7/10/16.
 */
public class PairingManager {
    private static final String TYPE_A = "/home/vontroy/vontroy/crypto/java-platform/pkuabe-web/src/main/resources/a.properties";

    public static Pairing getDefaultPairing() {
        InputStream inputStream = PairingManager.class.getClass().getResourceAsStream(TYPE_A);
        return PairingFactory.getPairing(new PropertiesParameters().load(inputStream));
    }
}
