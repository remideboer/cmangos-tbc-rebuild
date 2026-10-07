package org.tbc.world.session;

import org.tbc.world.entity.Player;

/**
 * One session's social scratch state: the pending group inviter (CMaNGOS Player::m_groupInvite) and the
 * open GM ticket text (single ticket per session in this rebuild).
 */
public final class SocialState {
    private Player pendingInvite;
    private String ticket = "";

    /** @return the player whose invite is awaiting accept/decline, or null. */
    public Player pendingInvite() {
        return pendingInvite;
    }

    public void invite(Player from) {
        pendingInvite = from;
    }

    /** @return the pending inviter and forget it (accept and decline both consume it). */
    public Player takePendingInvite() {
        Player from = pendingInvite;
        pendingInvite = null;
        return from;
    }

    public boolean hasTicket() {
        return !ticket.isEmpty();
    }

    /** @return open ticket text, "" when none. */
    public String ticket() {
        return ticket;
    }

    public void openTicket(String text) {
        ticket = text == null ? "" : text;
    }

    public void clearTicket() {
        ticket = "";
    }
}
