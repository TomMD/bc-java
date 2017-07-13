package org.bouncycastle.pqc.crypto.xmss;

import java.io.IOException;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.util.Arrays;

/**
 * XMSS^MT Private Key.
 */
public final class XMSSMTPrivateKeyParameters
    extends AsymmetricKeyParameter
    implements XMSSStoreableObjectInterface
{

    private final XMSSMTParameters params;
    private final long index;
    private final byte[] secretKeySeed;
    private final byte[] secretKeyPRF;
    private final byte[] publicSeed;
    private final byte[] root;
    private final BDSStateMap bdsState;

    private XMSSMTPrivateKeyParameters(Builder builder)
    {
        super(true);
        params = builder.params;
        if (params == null)
        {
            throw new NullPointerException("params == null");
        }
        int n = params.getDigestSize();
        byte[] privateKey = builder.privateKey;
        if (privateKey != null)
        {
            if (builder.xmss == null)
            {
                throw new NullPointerException("xmss == null");
            }
            /* import */
            int totalHeight = params.getHeight();
            int indexSize = (totalHeight + 7) / 8;
            int secretKeySize = n;
            int secretKeyPRFSize = n;
            int publicSeedSize = n;
            int rootSize = n;
            /*
            int totalSize = indexSize + secretKeySize + secretKeyPRFSize + publicSeedSize + rootSize;
			if (privateKey.length != totalSize) {
				throw new ParseException("private key has wrong size", 0);
			}
			*/
            int position = 0;
            index = XMSSUtil.bytesToXBigEndian(privateKey, position, indexSize);
            if (!XMSSUtil.isIndexValid(totalHeight, index))
            {
                throw new IllegalArgumentException("index out of bounds");
            }
            position += indexSize;
            secretKeySeed = XMSSUtil.extractBytesAtOffset(privateKey, position, secretKeySize);
            position += secretKeySize;
            secretKeyPRF = XMSSUtil.extractBytesAtOffset(privateKey, position, secretKeyPRFSize);
            position += secretKeyPRFSize;
            publicSeed = XMSSUtil.extractBytesAtOffset(privateKey, position, publicSeedSize);
            position += publicSeedSize;
            root = XMSSUtil.extractBytesAtOffset(privateKey, position, rootSize);
            position += rootSize;
			/* import BDS state */
            byte[] bdsStateBinary = XMSSUtil.extractBytesAtOffset(privateKey, position, privateKey.length - position);

            BDSStateMap bdsImport = null;
            try
            {
                bdsImport = (BDSStateMap)XMSSUtil.deserialize(bdsStateBinary);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            catch (ClassNotFoundException e)
            {
                e.printStackTrace();
            }
            bdsImport.setXMSS(builder.xmss);
            bdsState = bdsImport;
        }
        else
        {
			/* set */
            index = builder.index;
            byte[] tmpSecretKeySeed = builder.secretKeySeed;
            if (tmpSecretKeySeed != null)
            {
                if (tmpSecretKeySeed.length != n)
                {
                    throw new IllegalArgumentException("size of secretKeySeed needs to be equal size of digest");
                }
                secretKeySeed = tmpSecretKeySeed;
            }
            else
            {
                secretKeySeed = new byte[n];
            }
            byte[] tmpSecretKeyPRF = builder.secretKeyPRF;
            if (tmpSecretKeyPRF != null)
            {
                if (tmpSecretKeyPRF.length != n)
                {
                    throw new IllegalArgumentException("size of secretKeyPRF needs to be equal size of digest");
                }
                secretKeyPRF = tmpSecretKeyPRF;
            }
            else
            {
                secretKeyPRF = new byte[n];
            }
            byte[] tmpPublicSeed = builder.publicSeed;
            if (tmpPublicSeed != null)
            {
                if (tmpPublicSeed.length != n)
                {
                    throw new IllegalArgumentException("size of publicSeed needs to be equal size of digest");
                }
                publicSeed = tmpPublicSeed;
            }
            else
            {
                publicSeed = new byte[n];
            }
            byte[] tmpRoot = builder.root;
            if (tmpRoot != null)
            {
                if (tmpRoot.length != n)
                {
                    throw new IllegalArgumentException("size of root needs to be equal size of digest");
                }
                root = tmpRoot;
            }
            else
            {
                root = new byte[n];
            }
            BDSStateMap tmpBDSState = builder.bdsState;
            if (tmpBDSState != null)
            {
                bdsState = tmpBDSState;
            }
            else
            {
                bdsState = new BDSStateMap();
            }
        }
    }

    public static class Builder
    {

        /* mandatory */
        private final XMSSMTParameters params;
        /* optional */
        private long index = 0L;
        private byte[] secretKeySeed = null;
        private byte[] secretKeyPRF = null;
        private byte[] publicSeed = null;
        private byte[] root = null;
        private BDSStateMap bdsState = null;
        private byte[] privateKey = null;
        private XMSSParameters xmss = null;

        public Builder(XMSSMTParameters params)
        {
            super();
            this.params = params;
        }

        public Builder withIndex(long val)
        {
            index = val;
            return this;
        }

        public Builder withSecretKeySeed(byte[] val)
        {
            secretKeySeed = XMSSUtil.cloneArray(val);
            return this;
        }

        public Builder withSecretKeyPRF(byte[] val)
        {
            secretKeyPRF = XMSSUtil.cloneArray(val);
            return this;
        }

        public Builder withPublicSeed(byte[] val)
        {
            publicSeed = XMSSUtil.cloneArray(val);
            return this;
        }

        public Builder withRoot(byte[] val)
        {
            root = XMSSUtil.cloneArray(val);
            return this;
        }

        public Builder withBDSState(BDSStateMap val)
        {
            bdsState = val;
            return this;
        }

        public Builder withPrivateKey(byte[] privateKeyVal, XMSSParameters xmssVal)
        {
            privateKey = XMSSUtil.cloneArray(privateKeyVal);
            xmss = xmssVal;
            return this;
        }

        public XMSSMTPrivateKeyParameters build()
        {
            return new XMSSMTPrivateKeyParameters(this);
        }
    }

    public byte[] toByteArray()
    {
		/* index || secretKeySeed || secretKeyPRF || publicSeed || root */
        int n = params.getDigestSize();
        int indexSize = (params.getHeight() + 7) / 8;
        int secretKeySize = n;
        int secretKeyPRFSize = n;
        int publicSeedSize = n;
        int rootSize = n;
        int totalSize = indexSize + secretKeySize + secretKeyPRFSize + publicSeedSize + rootSize;
        byte[] out = new byte[totalSize];
        int position = 0;
		/* copy index */
        byte[] indexBytes = XMSSUtil.toBytesBigEndian(index, indexSize);
        XMSSUtil.copyBytesAtOffset(out, indexBytes, position);
        position += indexSize;
		/* copy secretKeySeed */
        XMSSUtil.copyBytesAtOffset(out, secretKeySeed, position);
        position += secretKeySize;
		/* copy secretKeyPRF */
        XMSSUtil.copyBytesAtOffset(out, secretKeyPRF, position);
        position += secretKeyPRFSize;
		/* copy publicSeed */
        XMSSUtil.copyBytesAtOffset(out, publicSeed, position);
        position += publicSeedSize;
		/* copy root */
        XMSSUtil.copyBytesAtOffset(out, root, position);
		/* concatenate bdsState */
        byte[] bdsStateOut = null;
        try
        {
            bdsStateOut = XMSSUtil.serialize(bdsState);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException("error serializing bds state");
        }
        return Arrays.concatenate(out, bdsStateOut);
    }

    public long getIndex()
    {
        return index;
    }

    public byte[] getSecretKeySeed()
    {
        return XMSSUtil.cloneArray(secretKeySeed);
    }

    public byte[] getSecretKeyPRF()
    {
        return XMSSUtil.cloneArray(secretKeyPRF);
    }

    public byte[] getPublicSeed()
    {
        return XMSSUtil.cloneArray(publicSeed);
    }

    public byte[] getRoot()
    {
        return XMSSUtil.cloneArray(root);
    }

    BDSStateMap getBDSState()
    {
        return bdsState;
    }

    public XMSSMTParameters getParameters()
    {
        return params;
    }

    public XMSSMTPrivateKeyParameters getNextKey()
    {
        final long globalIndex = this.getIndex();
        final int xmssHeight = params.getXMSSParameters().getHeight();

        //
        // set up state for next signature
        //
        long indexTree = XMSSUtil.getTreeIndex(globalIndex, xmssHeight);
        int indexLeaf = XMSSUtil.getLeafIndex(globalIndex, xmssHeight);

        OTSHashAddress otsHashAddress = (OTSHashAddress)new OTSHashAddress.Builder().withTreeAddress(indexTree)
            .withOTSAddress(indexLeaf).build();

        BDSStateMap newState = new BDSStateMap(bdsState);

        /* prepare authentication path for next leaf */
        if (indexLeaf < ((1 << xmssHeight) - 1))
        {
            if (newState.get(0) == null || indexLeaf == 0)
            {
                newState.put(0, new BDS(params.getXMSSParameters(), publicSeed, secretKeySeed, otsHashAddress));
            }

            newState.update(0, publicSeed, secretKeySeed, otsHashAddress);
        }

        /* loop over remaining layers */
        for (int layer = 1; layer < params.getLayers(); layer++)
        {
      			/* get root of layer - 1 */
            indexLeaf = XMSSUtil.getLeafIndex(indexTree, xmssHeight);
            indexTree = XMSSUtil.getTreeIndex(indexTree, xmssHeight);
      			/* adjust addresses */
            otsHashAddress = (OTSHashAddress)new OTSHashAddress.Builder().withLayerAddress(layer)
                .withTreeAddress(indexTree).withOTSAddress(indexLeaf).build();

      			/* prepare authentication path for next leaf */
            if (indexLeaf < ((1 << xmssHeight) - 1)
                && XMSSUtil.isNewAuthenticationPathNeeded(globalIndex, xmssHeight, layer))
            {
                if (newState.get(layer) == null)
                {
                    newState.put(layer, new BDS(params.getXMSSParameters(), publicSeed, secretKeySeed, otsHashAddress));
                }

                newState.update(layer, publicSeed, secretKeySeed, otsHashAddress);
            }
        }

        return new XMSSMTPrivateKeyParameters.Builder(params).withIndex(index + 1)
            .withSecretKeySeed(secretKeySeed).withSecretKeyPRF(secretKeyPRF)
            .withPublicSeed(publicSeed).withRoot(root)
            .withBDSState(newState).build();
    }
}
