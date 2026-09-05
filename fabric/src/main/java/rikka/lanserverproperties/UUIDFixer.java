package rikka.lanserverproperties;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class UUIDFixer {
	public static boolean tryOnlineFirst = false;
	public static List<String> alwaysOfflinePlayers = Collections.emptyList();

	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();

	/**
	 *  Mixin/ Coremod callback
	 */
	public static UUID hookEntry(String playerName) {
		if (alwaysOfflinePlayers.contains(playerName))
			return null;

		if (tryOnlineFirst)
			return getOfficialUUID(playerName);

		return null;
	}

	@Nullable
	public static UUID getOfficialUUID(String playerName) {
		String url = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.timeout(Duration.ofSeconds(10))
					.GET()
					.build();
			HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				return null;
			}

			String UUIDJson = response.body();
			if (!UUIDJson.isEmpty()) {
				JsonObject root = JsonParser.parseString(UUIDJson).getAsJsonObject();
				String playerName2 = root.getAsJsonPrimitive("name").getAsString();
				String uuidString = root.getAsJsonPrimitive("id").getAsString();
				// com.mojang.util.UUIDTypeAdapter.fromString(String)
				long uuidMSB = Long.parseLong(uuidString.substring(0, 8), 16);
				uuidMSB <<= 32;
				uuidMSB |= Long.parseLong(uuidString.substring(8, 16), 16);
				long uuidLSB = Long.parseLong(uuidString.substring(16, 24), 16);
				uuidLSB <<= 32;
				uuidLSB |= Long.parseLong(uuidString.substring(24, 32), 16);
				UUID uuid = new UUID(uuidMSB, uuidLSB);

				if (playerName2.equalsIgnoreCase(playerName))
					return uuid;
			}
		} catch (IOException | InterruptedException | JsonSyntaxException e) {
			e.printStackTrace();
		}

		return null;
	}
}
