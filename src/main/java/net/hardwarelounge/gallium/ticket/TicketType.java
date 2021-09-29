package net.hardwarelounge.gallium.ticket;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TicketType {

    MODERATION("Moderation", "mod", "\uD83D\uDEA8", """
            Fragen zum Regelwerk, andere User Melden, \
            oder gegen einen Regelverstoß von dir Berufung einlegen.
            """),

    GENERAL("Allgemein", "gen", "\uD83D\uDCE9", """
           Melde dich hier, wenn keine der anderen Kategorien zutreffen (z.B. Team-Bewerbungen). \
           Diese Kategorie ist nicht für allgemeinen Technik-Support gedacht.
           """),

    HARDWARE("Hardware", "hw", "\uD83D\uDD27", """
            Hardware-Problemen oder Fragen zu Neuanschaffungen.
            """),

    SOFTWARE("Software", "sw", "\uD83E\uDDD1\u200D\uD83D\uDCBB", """
            Fragen zum Thema Software. (z.B. Windows oder Spiel startet nicht)
            """),

    ESPORT("E-Sport", "esp", "\uD83C\uDFAE", """
            Fragen zu Turnieren oder HWL Esport Teams
            """)

    ;

    private final String name;
    private final String id;
    private final String emoji;
    private final String description;

}
