CodeNGamet website files

Main pages:
  index.html  - Home
  games.html  - Games catalogue
  apps.html   - Apps catalogue

Game and app detail pages:
  games/star-hopper.html
  Add future game pages directly inside games/ (for example, games/frog-valley.html).
  Add app pages directly inside apps/ (for example, apps/my-app.html).

Star Hopper downloads:
  Open Star Hopper using the single "View game" link. On its game page,
  choose Windows, Linux, or Android. The Download button stays faded until
  a platform is selected, then downloads that platform's release asset.

  Expected GitHub release tag: star-hopper
  Release assets configured in games/star-hopper.html:
    StarHopper-Setup.exe
    StarHopper-x86_64.AppImage
    StarHopper.apk

  Make sure the Windows and Android asset filenames above exactly match the
  files attached to the GitHub release. If your release uses different names,
  update the matching filenames in the platforms object in the page script.

Images:
  favicon.png belongs in the main/root folder.
  Put game and app icons in icons/ using lowercase hyphenated names, such as
  icons/star-hopper.png and icons/frog-valley.png.

  Put each game's screenshots in screenshots/<game-name>/ using numbered names:
  screenshots/star-hopper/1.png
  screenshots/star-hopper/2.png
  screenshots/star-hopper/3.png
