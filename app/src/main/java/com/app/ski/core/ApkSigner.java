package com.app.ski.core;

import android.content.Context;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;

public final class ApkSigner {

    private static final String KEYSTORE_FILE =
            "ski-signing.jks";

    private static final String KEY_ALIAS =
            "ski";

    private static final char[] PASSWORD =
            "ski-build-key-2026".toCharArray();

    private ApkSigner() {
    }

    public static void sign(
            Context context,
            File unsignedApk,
            File signedApk
    ) throws Exception {

        if (unsignedApk == null ||
                !unsignedApk.exists()) {

            throw new IllegalArgumentException(
                    "Unsigned APK does not exist."
            );
        }

        if (signedApk == null) {
            throw new IllegalArgumentException(
                    "Signed APK output is null."
            );
        }

        Security.removeProvider("BC");

        Security.addProvider(
                new BouncyCastleProvider()
        );

        KeyStore keyStore =
                loadOrCreateKeyStore(context);

        PrivateKey privateKey =
                (PrivateKey) keyStore.getKey(
                        KEY_ALIAS,
                        PASSWORD
                );

        X509Certificate certificate =
                (X509Certificate)
                        keyStore.getCertificate(
                                KEY_ALIAS
                        );

        if (privateKey == null ||
                certificate == null) {

            throw new Exception(
                    "SKI signing key could not be loaded."
            );
        }

        com.android.apksig.ApkSigner.SignerConfig
                signerConfig =
                new com.android.apksig.ApkSigner
                        .SignerConfig.Builder(
                        KEY_ALIAS,
                        privateKey,
                        Collections.singletonList(
                                certificate
                        )
                )
                        .build();

        com.android.apksig.ApkSigner signer =
                new com.android.apksig.ApkSigner.Builder(
                        Collections.singletonList(
                                signerConfig
                        )
                )
                        .setInputApk(
                                unsignedApk
                        )
                        .setOutputApk(
                                signedApk
                        )
                        .setV1SigningEnabled(true)
                        .setV2SigningEnabled(true)
                        .setV3SigningEnabled(true)
                        .setV4SigningEnabled(false)
                        .build();

        signer.sign();

        if (!signedApk.exists() ||
                signedApk.length() == 0) {

            throw new Exception(
                    "APK signing failed."
            );
        }
    }

    private static KeyStore loadOrCreateKeyStore(
            Context context
    ) throws Exception {

        File keyStoreFile =
                new File(
                        context.getFilesDir(),
                        KEYSTORE_FILE
                );

        KeyStore keyStore =
                KeyStore.getInstance(
                        KeyStore.getDefaultType()
                );

        if (keyStoreFile.exists()) {

            try (FileInputStream input =
                         new FileInputStream(
                                 keyStoreFile
                         )) {

                keyStore.load(
                        input,
                        PASSWORD
                );
            }

            return keyStore;
        }

        keyStore.load(
                null,
                PASSWORD
        );

        KeyPairGenerator generator =
                KeyPairGenerator.getInstance(
                        "RSA"
                );

        generator.initialize(2048);

        KeyPair keyPair =
                generator.generateKeyPair();

        X509Certificate certificate =
                createCertificate(keyPair);

        keyStore.setKeyEntry(
                KEY_ALIAS,
                keyPair.getPrivate(),
                PASSWORD,
                new X509Certificate[]{
                        certificate
                }
        );

        try (FileOutputStream output =
                     new FileOutputStream(
                             keyStoreFile
                     )) {

            keyStore.store(
                    output,
                    PASSWORD
            );
        }

        return keyStore;
    }

    private static X509Certificate createCertificate(
            KeyPair keyPair
    ) throws Exception {

        long now =
                System.currentTimeMillis();

        Date notBefore =
                new Date(
                        now - 60_000L
                );

        Date notAfter =
                new Date(
                        now +
                                1000L *
                                        60L *
                                        60L *
                                        24L *
                                        3650L
                );

        BigInteger serial =
                new BigInteger(
                        64,
                        new java.security.SecureRandom()
                ).abs();

        if (serial.equals(BigInteger.ZERO)) {
            serial = BigInteger.ONE;
        }

        X500Name subject =
                new X500Name(
                        "CN=SKI,O=SKI"
                );

        JcaX509v3CertificateBuilder builder =
                new JcaX509v3CertificateBuilder(
                        subject,
                        serial,
                        notBefore,
                        notAfter,
                        subject,
                        keyPair.getPublic()
                );

        ContentSigner certificateSigner =
                new JcaContentSignerBuilder(
                        "SHA256withRSA"
                )
                        .setProvider("BC")
                        .build(
                                keyPair.getPrivate()
                        );

        X509CertificateHolder holder =
                builder.build(
                        certificateSigner
                );

        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(holder);
    }
}