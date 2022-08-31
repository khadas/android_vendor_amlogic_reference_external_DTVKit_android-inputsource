package org.droidlogic.fvp;

import android.util.Log;
import java.util.Arrays;
import android.net.Uri;
import android.content.Context;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import android.content.res.Resources;
import java.io.InputStream;

public class FvpInterface {

    private static final String TAG = "FvpInterface";
    private static final String PkgAllowName = "com.vewd.core.fvpsign";
    private static final String PkgAllowNameForTrunk = "uk.co.freeview.signaidl";
    private static final String CONTENT_URI = "content://com.amazon.fvp.provider/mdsIntermediateCertificate";
    private static int mLocalPublicKeyId = -1;
    private Context mContext;

    private native byte[] native_signCSR(byte[] csr, byte[] intermediatePublicKey);
    static {
        System.loadLibrary("fvp_signcsr_jni");
    }

    public FvpInterface() {
        Log.d(TAG,"FvpInterface init");
    }

    public FvpInterface(int publicKeyResourceId) {
        Log.d(TAG,"FvpInterface init,publicKeyResourceId=" + publicKeyResourceId);
        mLocalPublicKeyId = publicKeyResourceId;
    }

     /**
     * Signs a Certificate Signing Request
     * @param csr   Certificate Signing Request
     * @param context   Android Context
     * @return  FVP Certificate or {@code null} if issues
     */
    public byte[] signCSR(byte[] csr, Context context) {

        // Check whether caller is allowed to call signCSR
        if (isCallerAllowed(context)) {
            byte[] intermediatePublicKey = null;
            // Retrieve Intermediate CA public key
            if (mLocalPublicKeyId == -1)
                intermediatePublicKey = retrievePublicKey(context);
            else
                intermediatePublicKey = retrievePublicKeyFromLocal(context, mLocalPublicKeyId);
            //byte[] intermediatePublicKey = new byte[1];
            // Use Trusty API to sign the CSR in Trusted Application
            byte[] signedCSR = signCSRTee(csr, intermediatePublicKey);

            if (mLocalPublicKeyId == -1)// Create certificate chain and return
                return createCertificateChain(signedCSR, intermediatePublicKey);
            else
                return signedCSR;
        }

        // Return null if any issues
        return null;
    }

    /**
     * Returns whether caller is allowed to call signCSR.
     * Currently only {@code com.vewd.core.fvpsign} should be allowed.
     * @param context   Android Context
     * @return  True if caller package is allowed to call signCSR, false otherwise
     */
    private boolean isCallerAllowed(Context context) {
        if (context == null) {
            Log.e(TAG, "Context null");
            return false;
        }

        mContext = context;
        String packageName = mContext.getPackageName();
        if (packageName.equals(PkgAllowName) || packageName.equals(PkgAllowNameForTrunk)) {
            return true;
        }

        Log.i(TAG, "Illegal packageName:" + packageName);
        return false;
    }

    /**
     * Calls the Amazon FVP Certificate Content Provider with uri
     * {@code content://com.amazon.fvp.provider/mdsIntermediateCertificate}.
     *
     * On success, a {@code Cursor} with a single value (i.e. single column and single row) is returned.
     * The certificate can be retrieved from the {@code Cursor} as a byte array using {@code Cursor.getBlob()}
     * and then serialized into a Certificate object. On error {@code null} is returned.
     *
     * Need to request {@code com.amazon.fvp.provider.permission.READ_PROVIDER} Android permission
     * for access.
     *
     * @param context   Android Context
     * @return  FVP Intermediate CA public key on success
     */
    private byte[] retrievePublicKey(Context context) {
        Cursor cursor = null;
        byte[] PublicKey = null;
        Uri KeyUri = Uri.parse(CONTENT_URI);

        if (context == null) {
            Log.e(TAG, "Context null");
            return null;
        }

        try {
            cursor = context.getContentResolver().query(KeyUri, null, null, null, null);
            if (cursor == null || cursor.getCount() == 0) {
                Log.e(TAG, "PublicKey count error");
                return null;
            }
            while (cursor.moveToNext()) {
                PublicKey = cursor.getBlob(0);
                break;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Get PublicKey exception = " + e.getMessage());
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Log.d(TAG, "PublicKey: " + Arrays.toString(PublicKey));
        return PublicKey;
    }

    /**
     * Calls the FVP Certificate public key with local resource
     *
     * @param context   Android Context
     * @param id   local resource id of public key
     * @return  FVP Intermediate CA public key on success; Return null if failed
     */
    private byte[] retrievePublicKeyFromLocal(Context context, int id) {
        byte[] PublicKey = null;

        try {
            InputStream input = context.getResources().openRawResource(id);
            PublicKey = new byte[input.available()];
            input.read(PublicKey);
            input.close();
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resources.NotFoundException: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "File read Exception: " + e.getMessage());
        }

        Log.d(TAG, "PublicKey: " + Arrays.toString(PublicKey));
        return PublicKey;
    }

    /**
     * Calls SoC Trusted Application in Trusted Execution Environment (TEE) using Trusty API to
     * sign a given {@code csr} using the given {@code intermediatePublicKey} and Intermediate CA
     * private key stored in TEE.
     *
     * Trusted Application should:
     * - Read the intermediate private key from the TEE
     * - Parse and validate the certificate signing request ({@code csr})
     * - Perform the certificate signing using appropriate cryptographic functions
     *
     * @param csr   Certificate Signing Request
     * @param intermediatePublicKey FVP Intermediate CA public key
     * @return  Signed Certificate Signing Request on success
     */
    private byte[] signCSRTee(byte[] csr, byte[] intermediatePublicKey) {
        Log.d(TAG,"Csr size = " + csr.length + ",PublicKey size = " + intermediatePublicKey.length);
        byte[] signedCSR = native_signCSR(csr, intermediatePublicKey);
        if (signedCSR != null && signedCSR.length >0) {
            Log.d(TAG, "native_signCSR success");
            printCSRData(signedCSR);
            return signedCSR;
        } else {
            Log.e(TAG, "native_signCSR failed");
        }
        return null;
    }

    /**
     * Concatenates the {@code signedCSR} and {@code intermediatePublicKey} together to form the
     * FVP certificate chain.
     * @param signedCSR A signed Certificate Signing Request
     * @param intermediatePublicKey FVP Intermediate CA public key
     * @return  FVP Certificate on success
     */
    private byte[] createCertificateChain(byte[] signedCSR, byte[] intermediatePublicKey) {
        if (signedCSR == null || intermediatePublicKey == null) {
            Log.e(TAG, "Certs data null");
            return null;
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
            outputStream.write( signedCSR );
            outputStream.write( intermediatePublicKey );

            byte chainCerts[] = outputStream.toByteArray( );
            return chainCerts;
        }
        catch (IOException e) {
            Log.e(TAG, "IO Exception Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    private void printCSRData(byte[] signedCSR){
    Log.d(TAG, "Get data from native, size:" + signedCSR.length);
    Log.d(TAG,"**********print data in**************");
    for (int i=0;i < signedCSR.length;i++) {
        System.out.printf("%02x ",signedCSR[i]);
        if (((i+1)%16) == 0)
            System.out.printf( "\n");
    }
    Log.d(TAG,"**********print data out**************");
    }
}
