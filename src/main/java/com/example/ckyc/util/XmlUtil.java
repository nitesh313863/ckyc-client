package com.example.ckyc.util;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.*;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;

public class XmlUtil {

    public static void signXml(Document doc, PrivateKey privateKey, X509Certificate cert, String version) throws Exception {
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        String normalizedVersion = normalizeVersion(version);
        String digestMethod = "1.2".equals(normalizedVersion) ? DigestMethod.SHA1 : DigestMethod.SHA256;
        String signatureMethod = "1.2".equals(normalizedVersion) ? SignatureMethod.RSA_SHA1 : SignatureMethod.RSA_SHA256;

        Reference ref = fac.newReference("",
                fac.newDigestMethod(digestMethod, null),
                Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                null, null);

        SignedInfo si = fac.newSignedInfo(
                fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                fac.newSignatureMethod(signatureMethod, null),
                Collections.singletonList(ref));

        KeyInfoFactory kif = fac.getKeyInfoFactory();
        X509Data xd = kif.newX509Data(
                java.util.List.of(cert.getSubjectX500Principal().getName(), cert)
        );
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(xd));

        DOMSignContext dsc = new DOMSignContext(privateKey, doc.getDocumentElement());

        fac.newXMLSignature(si, ki).sign(dsc);
    }

    public static boolean verifyXmlSignature(Document doc, PublicKey publicKey) throws Exception {
        NodeList sigNodes = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if (sigNodes.getLength() == 0) {
            return false;
        }
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        DOMValidateContext validateContext = new DOMValidateContext(publicKey, sigNodes.item(0));
        XMLSignature signature = fac.unmarshalXMLSignature(validateContext);
        return signature.validate(validateContext);
    }

    private static String normalizeVersion(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("CKYC version must be provided (expected 1.2 or 1.3)");
        }
        String normalized = version.trim();
        if (!"1.2".equals(normalized) && !"1.3".equals(normalized)) {
            throw new IllegalArgumentException("Unsupported CKYC version: " + normalized);
        }
        return normalized;
    }
}
