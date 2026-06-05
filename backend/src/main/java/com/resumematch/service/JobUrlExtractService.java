package com.resumematch.service;

import com.resumematch.dto.JobUrlExtractResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;

@Service
public class JobUrlExtractService {
    private static final Logger log = LoggerFactory.getLogger(JobUrlExtractService.class);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/125.0 Safari/537.36";
    private static final int TIMEOUT_MILLIS = 8000;
    private static final int MIN_CONTENT_LENGTH = 100;
    private static final int MAX_REDIRECTS = 3;
    private static final String FAILURE_MESSAGE = "채용공고 본문을 불러오지 못했습니다. 직접 입력 탭을 이용해주세요.";

    public JobUrlExtractResponse extract(String rawUrl) {
        String normalizedUrl = rawUrl == null ? "" : rawUrl.trim();

        try {
            URI uri = validatePublicHttpUrl(normalizedUrl);
            Document document = fetchDocument(uri);
            String title = normalizeText(document.title());
            String content = buildContent(document);

            if (content.length() < MIN_CONTENT_LENGTH) {
                log.warn("Job URL extract failed: content too short. url={}, length={}", normalizedUrl, content.length());
                return failure(normalizedUrl);
            }

            log.info("Job URL extract success: url={}, title={}, contentLength={}", normalizedUrl, title, content.length());
            return JobUrlExtractResponse.builder()
                    .success(true)
                    .url(normalizedUrl)
                    .title(title)
                    .content(content)
                    .message("채용공고 본문 추출이 완료되었습니다.")
                    .build();
        } catch (Exception e) {
            log.warn("Job URL extract failed: url={}, reason={}", normalizedUrl, e.getMessage());
            return failure(normalizedUrl);
        }
    }

    private Document fetchDocument(URI startUri) throws Exception {
        URI currentUri = startUri;

        for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
            org.jsoup.Connection.Response response = Jsoup.connect(currentUri.toString())
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MILLIS)
                    .followRedirects(false)
                    .ignoreHttpErrors(false)
                    .execute();

            int statusCode = response.statusCode();
            if (statusCode >= 300 && statusCode < 400) {
                String location = response.header("Location");
                if (location == null || location.isBlank()) {
                    throw new IllegalArgumentException("redirect location is empty");
                }

                currentUri = validatePublicHttpUrl(currentUri.resolve(location).toString());
                continue;
            }

            return response.parse();
        }

        throw new IllegalArgumentException("too many redirects");
    }

    private String buildContent(Document document) {
        document.select("script, style, noscript").remove();

        String title = normalizeText(document.title());
        String description = extractMetaDescription(document);
        String body = normalizeText(document.body() == null ? "" : document.body().text());

        return normalizeText(String.join("\n", title, description, body));
    }

    private String extractMetaDescription(Document document) {
        Elements candidates = document.select(
                "meta[name=description], meta[property=og:description], meta[name=twitter:description]"
        );

        return candidates.stream()
                .map(element -> normalizeText(element.attr("content")))
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse("");
    }

    private URI validatePublicHttpUrl(String rawUrl) throws Exception {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("url is empty");
        }

        URI uri = URI.create(rawUrl.trim());
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("unsupported url scheme");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("url host is empty");
        }

        validatePublicHost(host);
        return uri;
    }

    private void validatePublicHost(String host) throws Exception {
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (normalizedHost.equals("localhost") || normalizedHost.endsWith(".localhost")) {
            throw new IllegalArgumentException("localhost is blocked");
        }

        InetAddress[] addresses = InetAddress.getAllByName(normalizedHost);
        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                throw new IllegalArgumentException("private or local address is blocked");
            }
        }
    }

    private boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        if (address instanceof Inet6Address) {
            byte[] bytes = address.getAddress();
            int firstByte = bytes[0] & 0xff;
            return (firstByte & 0xfe) == 0xfc;
        }

        return false;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        return value.replace('\u00a0', ' ')
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll("\\n\\s*", "\n")
                .trim();
    }

    private JobUrlExtractResponse failure(String url) {
        return JobUrlExtractResponse.builder()
                .success(false)
                .url(url)
                .title("")
                .content("")
                .message(FAILURE_MESSAGE)
                .build();
    }
}
