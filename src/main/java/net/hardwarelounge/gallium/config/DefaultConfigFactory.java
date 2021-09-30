package net.hardwarelounge.gallium.config;

public abstract class DefaultConfigFactory {

    public static DefaultConfigFactory createDefault() {
        throw new IllegalStateException("A subclass of " + DefaultConfigFactory.class.getName()
                + " did not implement the static method createDefault()");
    }

}
