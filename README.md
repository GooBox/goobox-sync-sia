# goobox-sync-sia
[![Build Status](https://travis-ci.org/GooBox/goobox-sync-sia.svg?branch=master)](https://travis-ci.org/GooBox/goobox-sync-sia)
[![Build status](https://ci.appveyor.com/api/projects/status/j4lv9lnd07o1qe5n/branch/master?svg=true)](https://ci.appveyor.com/project/jkawamoto/goobox-sync-sia/branch/master)

sync app for [sia](https://sia.tech/).


## Prerequisites

- Install Java 8 or later. Make sure to install the 64-bit variant.
- Run [SIA daemon](https://github.com/NebulousLabs/Sia/releases) and listen on port 9980 (default)
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

- **username**: Goobox user name (currently any random string is acceptable, but should be unique in the SIA cloud)
- **primary-seed**: primary seed or encryption password of your SIA wallet

### Example

```
username = testuser@example.com
primary-seed = foo bar baz foo bar baz foo bar baz foo bar baz foo bar baz foo bar baz foo bar baz foo bar baz foo bar baz foo bar
```

## Usage

1. Open a Command Prompt / Terminal
2. Navigate to the extracted `goobox-sync-sia/bin` folder
3. Run the batch file, i.e. `goobox-sync-sia.bat` (for Windows), `goobox-sync-sia` (for Mac/Linux)


- Currently this app supports basic two-way sync from the `local file system -> SIA cloud` and `SIA cloud -> local file system` on the same machine only.
- The app will sync each content of which SIA path, which is a file path in the SIA cloud, starts with `<username>/Goobox`
to the local folder with name `Goobox`, which is a subfolder of the user home folder. If a file is present locally and not in the cloud it will create a file path and entry in the sia cloud and upload the file.
- The app will poll the SIA cloud and the local Goobox sync folder once per minute for any changes in the content. Aditionally a file watcher is implemented to watch for any changes in the local file system directory. 

---------------------------------------------------------------------------------------------------------------------------------------

The app uses an embedded Nitrine database for storing the current sync state of the files. The DB file can be found at the following location for each OS:

- `C:\Users\<user-name>\AppData\Local\Goobox` for Windows
- `~/.local/share/Goobox` for Linux
- `~/Library/Application Support/Goobox` for macOS

### Sub commands

`goobox-sync-sia.bat` and `goobox-sync-sia` have some sub commands;

```
usage: goobox-sync-sia.bat [-h] [--output-events] [--reset-db] [--sync-dir <arg>] [-v]

Sync app for Sia

    -h,--help             show this help
    --output-events       output events for the GUI app
    --reset-db            reset sync DB
    --sync-dir <arg>      set the sync dir
    -v,--version          print version

Commands:

 wallet               Show your wallet information
 create-allowance     Create allowance
 gateway-connect      Connects the gateway to a peer
 dump-db              Dump database for debugging
```

#### wallet command

`goobox-sync-sia.bat wallet` and `goobox-sync-sia wallet` print the following wallet information:

  - main wallet address
  - confirmed balance (in SC)
  - current spending
  - current prices (approx.)

#### create-allowance command

`goobox-sync-sia.bat create-allowance` and `goobox-sync-sia create-allowance` creates allowance. By default, this command adds your confirmed balance to the fund. You can choose hastings to be added to the fund with `--fund` flag, e.g. `goobox-sync-sia.bat create-allowance --fund 100000` adds 100000 hastings to your current fund.

## License
This software is released under The GNU General Public License Version 3, see [LICENSE](LICENSE) for more detail.
