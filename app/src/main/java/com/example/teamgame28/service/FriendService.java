package com.example.teamgame28.service;

import com.example.teamgame28.repository.FriendRepository;

public class FriendService {

    private final FriendRepository friendRepository;

    public FriendService() {
        this.friendRepository = new FriendRepository();
    }

    /**
     * Pošalji zahtev za prijateljstvo.
     */
    public void sendFriendRequest(String fromUserId, String toUserId, ServiceCallback callback) {
        friendRepository.sendFriendRequest(fromUserId, toUserId, new FriendRepository.RepoCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess("Zahtev uspešno poslat!");
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure("Greška: " + e.getMessage());
            }
        });
    }

    /**
     * Prihvati zahtev za prijateljstvo.
     */
    public void acceptFriendRequest(String requestId, String fromUserId, String toUserId, ServiceCallback callback) {
        friendRepository.acceptFriendRequest(requestId, fromUserId, toUserId, new FriendRepository.RepoCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess("Prijateljstvo prihvaćeno!");
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure("Greška: " + e.getMessage());
            }
        });
    }

    /**
     * Odbij zahtev za prijateljstvo.
     */
    public void declineFriendRequest(String requestId, ServiceCallback callback) {
        friendRepository.declineFriendRequest(requestId, new FriendRepository.RepoCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess("Zahtev odbijen.");
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure("Greška: " + e.getMessage());
            }
        });
    }

    // Generic service callback
    public interface ServiceCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }
}
