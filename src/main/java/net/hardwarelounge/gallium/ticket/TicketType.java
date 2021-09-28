package net.hardwarelounge.gallium.ticket;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TicketType {

    MODERATION("Moderation", "rotating_light", """
            Fragen zum Regelwerk, andere User Melden,
            oder gegen einen Regelverstoß von dir Berufung einlegen.
            """),

    GENERAL("Allgemein", "envelope_with_arrow", """
           Melde dich hier, wenn keine der anderen Kategorien zutreffen (z.B. Team-Bewerbungen).
           Diese Kategorie ist nicht für allgemeinen Technik-Support gedacht.
           """),

    HARDWARE("Hardware", "wrench", """
            Hardware-Problemen oder Fragen zu Neuanschaffungen.
            """),

    SOFTWARE("Software", "man_technologist", """
            Fragen zum Thema Software. (z.B. Windows oder Spiel startet nicht)
            """),

    ESPORT("E-Sport", "video_game", """
            Fragen zu Turnieren oder HWL Esport Teams
            """)

    ;

    private final String name;
    private final String emoji;
    private final String description;

}
