package com.hideseek.cachecache.game;

public enum GameState {
    LOBBY,      // en attente de joueurs
    COUNTDOWN,  // compte à rebours avant lancement
    STARTING,   // seeker aveugle/immobile (15s)
    RUNNING,    // partie en cours
    ENDING      // écran de fin (10s) avant téléport au hub
}
