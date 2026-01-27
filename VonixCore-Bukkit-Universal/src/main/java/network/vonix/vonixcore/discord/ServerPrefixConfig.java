package network.vonix.vonixcore.discord;

import network.vonix.vonixcore.VonixCore;
import org.javacord.api.entity.server.Server;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Configuration system for managing server prefixes in Discord advancement messages.
 */
public class ServerPrefixConfig {
    
    private static final String DEFAULT_PREFIX = "MC";
    private static final int MAX_PREFIX_LENGTH = 16;
    
    private final Map<Long, String> serverPrefixMap = new ConcurrentHashMap<>();
    private final Set<String> usedPrefixes = ConcurrentHashMap.newKeySet();
    private String fallbackPrefix = DEFAULT_PREFIX;
    private int prefixCounter = 1;
    
    public String getServerPrefix(long serverId) {
        return serverPrefixMap.computeIfAbsent(serverId, this::generateUniquePrefix);
    }
    
    public String getServerPrefix(Server server) {
        if (server == null) return getFallbackPrefix();
        return getServerPrefix(server.getId());
    }
    
    public void setServerPrefix(long serverId, String prefix) {
        if (!isValidPrefix(prefix)) {
            throw new IllegalArgumentException("Invalid prefix: " + prefix);
        }
        
        String normalizedPrefix = normalizePrefix(prefix);
        if (usedPrefixes.contains(normalizedPrefix)) {
            String currentPrefix = serverPrefixMap.get(serverId);
            if (currentPrefix == null || !normalizedPrefix.equals(normalizePrefix(currentPrefix))) {
                throw new IllegalArgumentException("Prefix '" + prefix + "' is already in use");
            }
        }
        
        String oldPrefix = serverPrefixMap.get(serverId);
        if (oldPrefix != null) {
            usedPrefixes.remove(normalizePrefix(oldPrefix));
        }
        
        serverPrefixMap.put(serverId, prefix);
        usedPrefixes.add(normalizedPrefix);
        VonixCore.getInstance().getLogger().log(Level.INFO, 
            "[Discord] Set server prefix for server " + serverId + " to '" + prefix + "'");
    }
    
    public String getFallbackPrefix() {
        return fallbackPrefix;
    }
    
    public void setFallbackPrefix(String fallbackPrefix) {
        if (!isValidPrefix(fallbackPrefix)) {
            throw new IllegalArgumentException("Invalid fallback prefix: " + fallbackPrefix);
        }
        this.fallbackPrefix = fallbackPrefix;
        VonixCore.getInstance().getLogger().log(Level.INFO, 
            "[Discord] Set fallback prefix to '" + fallbackPrefix + "'");
    }
    
    public Map<Long, String> getAllServerPrefixes() {
        return new HashMap<>(serverPrefixMap);
    }
    
    public void clearAllPrefixes() {
        serverPrefixMap.clear();
        usedPrefixes.clear();
        prefixCounter = 1;
    }
    
    private boolean isValidPrefix(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) return false;
        String trimmed = prefix.trim();
        if (trimmed.length() > MAX_PREFIX_LENGTH) return false;
        return trimmed.matches("[a-zA-Z0-9\\-_\\[\\]\\(\\)\\{\\}]+");
    }
    
    private String normalizePrefix(String prefix) {
        return prefix.trim().toLowerCase();
    }
    
    private String generateUniquePrefix(long serverId) {
        String basePrefix = "S" + Math.abs(serverId % 1000);
        String candidatePrefix = basePrefix;
        
        while (usedPrefixes.contains(normalizePrefix(candidatePrefix))) {
            candidatePrefix = basePrefix + "_" + prefixCounter++;
            if (prefixCounter > 9999) {
                candidatePrefix = "MC" + System.currentTimeMillis() % 1000;
                break;
            }
        }
        
        if (candidatePrefix.length() > MAX_PREFIX_LENGTH) {
            candidatePrefix = candidatePrefix.substring(0, MAX_PREFIX_LENGTH);
        }
        
        usedPrefixes.add(normalizePrefix(candidatePrefix));
        VonixCore.getInstance().getLogger().log(Level.INFO, 
            "[Discord] Generated unique prefix '" + candidatePrefix + "' for server " + serverId);
        return candidatePrefix;
    }
}
