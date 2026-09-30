package dev.aincrad.client;

/** Plain launcher supports the packaged classpath distribution without a system JavaFX install. */
public final class Launcher {
    private Launcher() {}
    public static void main(String[] args) {javafx.application.Application.launch(AincradApp.class,args);}
}
