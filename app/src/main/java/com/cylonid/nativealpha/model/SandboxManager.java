package com.cylonid.nativealpha.model;

import timber.log.Timber;
import androidx.annotation.NonNull;

import java.util.stream.IntStream;

public class SandboxManager {

	private static SandboxManager instance = null;
	private static final int NUM_OF_SANDBOXES = 8;
	private final Sandbox[] sandboxes = new Sandbox[NUM_OF_SANDBOXES];
	private int next_container;

	private SandboxManager() {
		next_container = 0;
		IntStream.range(0, NUM_OF_SANDBOXES).forEach(x -> sandboxes[x] = new Sandbox());
	}

	public static synchronized SandboxManager getInstance() {
		if (instance == null) {
			instance = new SandboxManager();
		}
		return instance;
	}

	public int calculateNextFreeContainerId() {
		if (next_container < 0 || next_container >= NUM_OF_SANDBOXES) {
			next_container = 0;
		}
		return next_container++;
	}

	private static boolean isValidSandboxId(int sandboxId) {
		return sandboxId >= 0 && sandboxId < NUM_OF_SANDBOXES;
	}

	public void registerWebAppToSandbox(WebApp webapp) {
		int sId = webapp.getContainerId();
		if (!isValidSandboxId(sId)) {
			Timber.w("Ignoring registerWebAppToSandbox with out-of-range containerId: %d", sId);
			return;
		}
		sandboxes[sId].registerWebApp(webapp.getID(), webapp.getBaseUrl());
		Timber.d("Sandbox %d is occupied by %s", webapp.getContainerId(), webapp.getTitle());
	}

	public void unregisterWebAppFromSandbox(int sandboxId) {
		if (!isValidSandboxId(sandboxId)) {
			Timber.w("Ignoring unregisterWebAppFromSandbox with out-of-range sandboxId: %d", sandboxId);
			return;
		}
		if (!sandboxes[sandboxId].isUnoccupied()) {
			WebApp currently_set_webapp = DataManager.getInstance()
					.getWebApp(sandboxes[sandboxId].getCurrentlyRegisteredWebapp());
			sandboxes[sandboxId].unregisterWebApp();
			String title = currently_set_webapp != null ? currently_set_webapp.getTitle() : "<unknown>";
			Timber.d("Sandbox %d is left by %s", sandboxId, title);
		}
	}

	public boolean isSandboxUsedByAnotherApp(@NonNull WebApp webapp) {
		int sId = webapp.getContainerId();
		if (!isValidSandboxId(sId)) {
			return false;
		}
		return sandboxes[sId].isUsedByAnotherApp(webapp);
	}

	public int getNextContainer() {
		return next_container;
	}

	public void setNextContainer(int nextContainer) {
		if (nextContainer < 0 || nextContainer >= NUM_OF_SANDBOXES) {
			this.next_container = 0;
		} else {
			this.next_container = nextContainer;
		}
	}

}
