package com.example.alexahttps;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AlexaSignatureVerifier {

  private final String skillId;
  private final long toleranceSeconds;
  private final ObjectMapper mapper = new ObjectMapper();

  private static class CachedCert {
    X509Certificate cert;
    Instant fetchedAt;
  }
  private final ConcurrentHashMap<String, CachedCert> cache = new ConcurrentHashMap<>();

  public AlexaSignatureVerifier(
      @Value("${app.skill-id}") String skillId,
      @Value("${app.timestamp-tolerance:150}") long toleranceSeconds
  ) {
    this.skillId = skillId;
    this.toleranceSeconds = toleranceSeconds;
  }

  public void verify(String certUrl, String signatureB64, byte[] body) throws Exception {
    // 1) Validate URL
    URI uri = URI.create(certUrl);
    if (!"https".equalsIgnoreCase(uri.getScheme())) throw new SecurityException("cert url not https");
    if (!"s3.amazonaws.com".equalsIgnoreCase(uri.getHost())) throw new SecurityException("invalid cert host");
    if (!uri.getPath().startsWith("/echo.api/")) throw new SecurityException("invalid cert path");

    // 2) Fetch cert (cache 1h)
    X509Certificate cert = getCert(certUrl);

    // 3) Validate time & SAN
    cert.checkValidity(new Date());
    boolean sanOk = false;
    var sans = cert.getSubjectAlternativeNames();
    if (sans != null) {
      for (var it : sans) {
        Object val = it.get(1);
        if (val != null && val.toString().equals("echo-api.amazon.com")) { sanOk = true; break; }
      }
    }
    if (!sanOk) throw new SecurityException("SAN missing echo-api.amazon.com");

    // 4) Verify signature over body
    byte[] sigBytes = Base64.getDecoder().decode(signatureB64);
    PublicKey pk = cert.getPublicKey();

    boolean ok = verifyWithAlgo("SHA256withRSA", pk, body, sigBytes) || verifyWithAlgo("SHA1withRSA", pk, body, sigBytes);
    if (!ok) throw new SecurityException("signature invalid");
  }

  private X509Certificate getCert(String url) throws Exception {
    CachedCert c = cache.get(url);
    if (c != null && c.fetchedAt.isAfter(Instant.now().minus(Duration.ofHours(1)))) return c.cert;

    URL u = new URL(url);
    HttpsURLConnection conn = (HttpsURLConnection) u.openConnection();
    conn.setConnectTimeout(3000);
    conn.setReadTimeout(5000);
    conn.connect();
    try (InputStream is = conn.getInputStream()) {
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      Certificate cert = cf.generateCertificate(is);
      X509Certificate x = (X509Certificate) cert;
      c = new CachedCert(); c.cert = x; c.fetchedAt = Instant.now();
      cache.put(url, c);
      return x;
    }
  }

  public void assertAppAndTimestamp(byte[] body) throws Exception {
    AlexaDtos.RequestEnvelope env = mapper.readValue(new ByteArrayInputStream(body), AlexaDtos.RequestEnvelope.class);
    String appId = null;
    if (env.session != null && env.session.application != null) appId = env.session.application.applicationId;
    if ((appId == null || appId.isBlank()) && env.context != null && env.context.System != null && env.context.System.application != null) {
      appId = env.context.System.application.applicationId;
    }
    if (appId == null || !appId.equals(skillId)) throw new SecurityException("applicationId mismatch");

    if (env.request == null || env.request.timestamp == null) throw new SecurityException("timestamp missing");
    Instant ts = env.request.timestamp.toInstant();
    Instant now = Instant.now();
    long diff = Math.abs(now.getEpochSecond() - ts.getEpochSecond());
    if (diff > toleranceSeconds) throw new SecurityException("timestamp out of range");
  }

  private boolean verifyWithAlgo(String algo, PublicKey pk, byte[] body, byte[] sigBytes) {
    try {
      Signature sig = Signature.getInstance(algo);
      sig.initVerify(pk);
      sig.update(body);
      return sig.verify(sigBytes);
    } catch (Exception e) {
      return false;
    }
  }
}
