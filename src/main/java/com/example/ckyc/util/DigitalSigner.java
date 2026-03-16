package com.example.ckyc.util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class DigitalSigner {

    private static final String KEY_STORE_TYPE = "PKCS12";
    private final KeyStore.PrivateKeyEntry keyEntry;

    public DigitalSigner(String keyStoreFile, char[] keyStorePassword, String alias) {
        this.keyEntry = getKeyFromKeyStore(keyStoreFile, keyStorePassword, alias);
        if (this.keyEntry == null) {
            throw new RuntimeException(
                    "Key could not be read for digital signature. Please check value of signature alias and signature password."
            );
        }
    }

    public DigitalSigner(String keyStoreFile, char[] keyStorePassword) {
        this(keyStoreFile, keyStorePassword, null);
    }

    public String signXML(String xmlDocument, boolean includeKeyInfo, String version) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document inputDocument = dbf.newDocumentBuilder().parse(
                    new InputSource(new StringReader(xmlDocument)));

            Document signedDocument = sign(inputDocument, includeKeyInfo, version);

            StringWriter stringWriter = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer trans = tf.newTransformer();
            trans.transform(new DOMSource(signedDocument), new StreamResult(stringWriter));

            return stringWriter.getBuffer().toString();
        } catch (Exception e) {
            throw new RuntimeException("Error while digitally signing the XML document", e);
        }
    }

    public KeyStore.PrivateKeyEntry getKeyEntry() {
        return keyEntry;
    }

    private Document sign(Document xmlDoc, boolean includeKeyInfo, String version) throws Exception {
        if (System.getenv("SKIP_DIGITAL_SIGNATURE") != null) {
            return xmlDoc;
        }

        SignedInfo sInfo;
        String normalizedVersion = version == null ? "" : version.trim();
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        if ("1.2".equals(normalizedVersion)) {
            Reference ref = fac.newReference("", fac.newDigestMethod(
                    "http://www.w3.org/2000/09/xmldsig#sha1", null), Collections.singletonList(fac
                    .newTransform("http://www.w3.org/2000/09/xmldsig#enveloped-signature",
                            (XMLStructure) null)), null, null);

            sInfo = fac.newSignedInfo(fac.newCanonicalizationMethod(
                            "http://www.w3.org/TR/2001/REC-xml-c14n-20010315",
                            (XMLStructure) null), fac
                            .newSignatureMethod("http://www.w3.org/2000/09/xmldsig#rsa-sha1", null),
                    Collections.singletonList(ref));
        } else if ("1.3".equals(normalizedVersion)) {
            Reference ref = fac.newReference("", fac.newDigestMethod(
                    "http://www.w3.org/2001/04/xmlenc#sha256", null), Collections.singletonList(fac
                    .newTransform("http://www.w3.org/2000/09/xmldsig#enveloped-signature",
                            (XMLStructure) null)), null, null);

            sInfo = fac.newSignedInfo(fac.newCanonicalizationMethod(
                            "http://www.w3.org/TR/2001/REC-xml-c14n-20010315",
                            (XMLStructure) null), fac
                            .newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null),
                    Collections.singletonList(ref));
        } else {
            throw new IllegalArgumentException("Unsupported CKYC version for signature: " + normalizedVersion);
        }

        if (this.keyEntry == null) {
            throw new RuntimeException(
                    "Key could not be read for digital signature. Please check value of signature alias and signature password."
            );
        }

        X509Certificate x509Cert = (X509Certificate) this.keyEntry.getCertificate();

        KeyInfo kInfo = getKeyInfo(x509Cert, fac);
        DOMSignContext dsc = new DOMSignContext(this.keyEntry.getPrivateKey(),
                xmlDoc.getDocumentElement());
        XMLSignature signature = fac.newXMLSignature(sInfo,
                includeKeyInfo ? kInfo : null);
        signature.sign(dsc);

        Node node = dsc.getParent();
        return node.getOwnerDocument();
    }

    private KeyInfo getKeyInfo(X509Certificate cert, XMLSignatureFactory fac) {
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        List<Object> x509Content = new ArrayList<>();
        x509Content.add(cert.getSubjectX500Principal().getName());
        x509Content.add(cert);
        X509Data xd = kif.newX509Data(x509Content);
        return kif.newKeyInfo(Collections.singletonList(xd));
    }

    public KeyStore.PrivateKeyEntry getKeyFromKeyStore(String keyStoreFile, char[] keyStorePassword, String alias) {
        try (InputStream keyFileStream = open(keyStoreFile)) {
            KeyStore ks = KeyStore.getInstance(KEY_STORE_TYPE);
            ks.load(keyFileStream, keyStorePassword);

            String resolvedAlias = resolveAlias(ks, alias);
            return (PrivateKeyEntry) ks.getEntry(resolvedAlias,
                    new KeyStore.PasswordProtection(keyStorePassword));
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveAlias(KeyStore ks, String alias) throws Exception {
        if (alias != null && !alias.isBlank()) {
            return alias;
        }
        Enumeration<String> aliases = ks.aliases();
        if (ks.size() > 1) {
            throw new Exception("More than 1 key found in the keystore. Please specify key alias.");
        }
        if (!aliases.hasMoreElements()) {
            throw new Exception("Keystore is empty.");
        }
        return aliases.nextElement();
    }

    private InputStream open(String path) throws IOException {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Keystore path must not be blank");
        }
        String normalized = path.trim();
        if (normalized.startsWith("classpath:")) {
            String resource = normalized.substring("classpath:".length());
            if (resource.startsWith("/")) {
                resource = resource.substring(1);
            }
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
            if (in == null) {
                throw new IllegalArgumentException("Classpath resource not found: " + normalized);
            }
            return in;
        }
        return new FileInputStream(normalized);
    }
}
