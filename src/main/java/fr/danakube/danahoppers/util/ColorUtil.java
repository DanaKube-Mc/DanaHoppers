package fr.danakube.danahoppers.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Utilitaires de conversion et de désérialisation de chaînes MiniMessage (Adventure API).
 */
public final class ColorUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private ColorUtil() {
        throw new UnsupportedOperationException("Classe utilitaire instanciable non autorisée.");
    }

    /**
     * Désérialise une chaîne au format MiniMessage en Component.
     *
     * @param text le texte à désérialiser
     * @return le Component résolvant MiniMessage
     */
    public static Component parse(String text) {
        return parse(text, Collections.emptyMap());
    }

    /**
     * Désérialise une chaîne au format MiniMessage en Component avec des placeholders.
     *
     * @param text         le texte avec des balises MiniMessage et/ou placeholders
     * @param placeholders la map des clés-valeurs de remplacement (ex: "player" -> "Steve")
     * @return le Component formaté
     */
    public static Component parse(String text, Map<String, String> placeholders) {
        if (text == null) {
            return Component.empty();
        }
        if (placeholders == null || placeholders.isEmpty()) {
            return MINI_MESSAGE.deserialize(text).decoration(TextDecoration.ITALIC, false);
        }

        String processedText = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String val = entry.getValue() != null ? entry.getValue() : "";
            processedText = processedText.replace("%" + entry.getKey() + "%", val);
        }

        return MINI_MESSAGE.deserialize(processedText).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Convertit une liste de chaînes MiniMessage en liste de Components.
     *
     * @param textList la liste des lignes de texte
     * @return la liste des Components formatés
     */
    public static List<Component> parseList(List<String> textList) {
        return parseList(textList, Collections.emptyMap());
    }

    /**
     * Convertit une liste de chaînes MiniMessage en liste de Components avec des placeholders.
     *
     * @param textList     la liste des lignes de texte
     * @param placeholders la map des clés-valeurs de remplacement
     * @return la liste des Components formatés
     */
    public static List<Component> parseList(List<String> textList, Map<String, String> placeholders) {
        if (textList == null) {
            return Collections.emptyList();
        }
        List<Component> components = new ArrayList<>(textList.size());
        for (String line : textList) {
            components.add(parse(line, placeholders));
        }
        return components;
    }

    /**
     * Désérialise un texte en y préfixant le prefix du plugin.
     *
     * @param prefix       le préfixe MiniMessage
     * @param text         le texte du message
     * @param placeholders les placeholders éventuels
     * @return le Component combinant prefix + message
     */
    public static Component parseWithPrefix(String prefix, String text, Map<String, String> placeholders) {
        String fullText = (prefix != null ? prefix : "") + (text != null ? text : "");
        return parse(fullText, placeholders);
    }

    /**
     * Désérialise un texte en y préfixant le prefix du plugin.
     *
     * @param prefix le préfixe MiniMessage
     * @param text   le texte du message
     * @return le Component combinant prefix + message
     */
    public static Component parseWithPrefix(String prefix, String text) {
        return parseWithPrefix(prefix, text, Collections.emptyMap());
    }
}
