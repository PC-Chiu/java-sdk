package org.fisco.bcos.sdk.codec.datatypes.generated;

import java.math.BigInteger;
import org.fisco.bcos.sdk.codec.datatypes.Uint;

/**
 * Auto generated code.
 *
 * <p><strong>Do not modifiy!</strong>
 *
 * <p>Please use AbiTypesGenerator in the <a
 * href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 */
public class Uint136 extends Uint {
    public static final Uint136 DEFAULT = new Uint136(BigInteger.ZERO);

    public Uint136(BigInteger value) {
        super(136, value);
    }

    public Uint136(long value) {
        this(BigInteger.valueOf(value));
    }
}
