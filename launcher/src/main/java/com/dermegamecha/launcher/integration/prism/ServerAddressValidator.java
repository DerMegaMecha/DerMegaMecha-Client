package com.dermegamecha.launcher.integration.prism;

import java.io.IOException;
import java.net.IDN;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.regex.Pattern;

public final class ServerAddressValidator {
    private static final Pattern HOSTNAME = Pattern.compile(
            "(?i)^(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\\.(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*\\.?$");

    private ServerAddressValidator() {}

    public static String validate(String address) throws IOException {
        if (address == null || address.isBlank() || !address.equals(address.trim())
                || address.length() > 300 || address.contains("://")
                || address.chars().anyMatch(Character::isWhitespace)) {
            throw new IOException("Enter a server as hostname or address, optionally followed by :port.");
        }

        String host;
        String portText = null;
        if (address.startsWith("[")) {
            int closing = address.indexOf(']');
            if (closing < 0) throw invalid();
            host = address.substring(1, closing);
            String suffix = address.substring(closing + 1);
            if (!suffix.isEmpty()) {
                if (!suffix.startsWith(":")) throw invalid();
                portText = suffix.substring(1);
            }
            if (!host.contains(":") || !host.matches("(?i)[0-9a-f:.]+")) throw invalid();
            try {
                if (!(InetAddress.getByName(host) instanceof Inet6Address)) throw invalid();
            } catch (IllegalArgumentException ex) {
                throw invalid();
            }
        } else {
            int colon = address.lastIndexOf(':');
            if (colon >= 0) {
                if (address.indexOf(':') != colon) throw invalid();
                host = address.substring(0, colon);
                portText = address.substring(colon + 1);
            } else {
                host = address;
            }
            String asciiHost;
            try {
                asciiHost = IDN.toASCII(host);
            } catch (IllegalArgumentException ex) {
                throw invalid();
            }
            if (!HOSTNAME.matcher(asciiHost).matches()) throw invalid();
            host = asciiHost;
        }

        if (portText != null) {
            if (!portText.matches("[0-9]{1,5}")) throw invalid();
            int port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) throw invalid();
        }
        return address;
    }

    public static boolean isValid(String address) {
        try {
            validate(address);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private static IOException invalid() {
        return new IOException("The server address is invalid. Use a hostname or address, optionally followed by :port.");
    }
}
