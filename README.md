# goobox-sync-sia
[![Build Status](https://travis-ci.org/GooBox/goobox-sync-sia.svg?branch=master)](https://travis-ci.org/GooBox/goobox-sync-sia)

sync app for [sia](https://sia.tech/).


## Prerequisites
- Install Java 8 or later. Make sure to install the 64-bit variant.
- Run [SIA daemon](https://github.com/NebulousLabs/Sia/releases) and listen port 9980 (default)
  - If you're running SIA-UI, it runs SIA daemon with the above requirement.
- Setup a wallet
  - Initialize the wallet
  - Allocate funds


## Installation
1. Download the ZIP package of [the latest release](https://github.com/GooBox/goobox-sync-sia/releases)
2. Extract the ZIP on the local file system


## Configuration file
This app requires a Java property file `goobox.properties` in the following location:

- `C:\Users\<user-name>\AppData\Local\Goobox` for Windows (Note that <user-name> is a user name in Windows, not Goobox)
- `~/.local/share/Goobox` for Linux
- `~/Library/Application Support/Goobox` for macOS

The property file takes two parameters:

- username: Goobox user name (currently any random string is acceptable, but should be unique in the SIA cloud)
- primary-seed: primary seed or encryption password of your SIA wallet

### Example
```
username = testuser@example.com
primary-seed = foo bar baz foo bar baz foo bar baz foo bar baz foo bar baz foo bar baz foo bar baz foo bar baz foo bar baz foo bar
```


## Usage
1. Open a Command Prompt / Terminal
2. Navigate to the extracted `goobox-sync-sia/bin` folder
3. Run the batch file, i.e. `goobox-sync-sia.bat` (for Windows), `goobox-sync-sia` (for Mac/Linux)


Currently this app supports basic one-way sync from the SIA cloud to the local file system.
The app will sync each content of which SIA path, which is a file path in the SIA cloud, starts with `<username>/Goobox`
to the local folder with name `Goobox`, which is a subfolder of the user home folder.

The app will poll the SIA cloud and the local Goobox sync folder once per minute for any changes in the content.

The app uses an embedded Nitrine database for storing the current sync state of the files. The DB file can be found at the following location:

- `C:\Users\<user-name>\AppData\Local\Goobox` for Windows
- `~/.local/share/Goobox` for Linux
- `~/Library/Application Support/Goobox` for macOS

### Sub commands
`goobox-sync-sia.bat` and `goobox-sync-sia` have some sub commands.

#### wallet command
`goobox-sync-sia.bat wallet` and `goobox-sync-sia wallet` print the following your wallet information:

  - the main wallet address
  - confirmed balance (in SC)
  - current spending
  - current prices (approx.)

#### create-allowance command
`goobox-sync-sia.bat create-allowance` and `goobox-sync-sia create-allowance` creates allowance. By default, this command adds your confirmed balance to the fund. You can choose hastings to be added to the fund with `--fund` flag, e.g. `goobox-sync-sia.bat create-allowance --fund 100000` add 100000 hastings to your current fund.

## License
This software is released under The GNU General Public License Version 3, see [LICENSE](LICENSE) for more detail.
