# HandOff Android prototype
Open this directory in Android Studio with JDK 17, SDK 35 and Gradle 8.9. Build the debug APK or run on an Android phone.
## Laptop companion
Install rclone and OpenSSH tools. Generate a dedicated server key with ssh-keygen -t ed25519 -f handoff_key (empty passphrase for this prototype, protect the key file). Read its fingerprint with ssh-keygen -lf handoff_key.pub.
Run the following from the key directory, substituting a private VPN address assigned to the laptop, your folder and a strong password.
```
rclone serve sftp "/path/to/shared-folder" --addr VPN_IP:2022 --key handoff_key --user handoff --pass YOUR_PASSWORD
```
Connect both devices to the same private VPN (for example Tailscale) for internet access. For local Wi-Fi use the laptop LAN address instead. In HandOff enter that address, handoff, the password and the SHA256 fingerprint. Keep the laptop awake. Choose Send from phone or tap a laptop file. Move requires deletion permission and reads back content for verification. Use files that are not being changed by another program. On interrupted downloads the phone may retain a partial file. Transfers currently run while the activity stays open. This prototype has no resume or speed meter.
