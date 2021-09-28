package net.hardwarelounge.gallium.util;

/**
 * Exception indicating a {@link net.hardwarelounge.gallium.command.SlashCommand} failed
 *
 * @author Christian Schliz
 */
public class CommandFailedException extends RuntimeException {

    public CommandFailedException() {
        this("Es ist ein Fehler aufgetreten");
    }

    public CommandFailedException(String message) {
        super(message);
    }

}