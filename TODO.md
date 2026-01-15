- ORC scoring support
  - Boats can be either (PHRF, PHRF + ORC, ORC, Windseeker FS, Windseeker)
    - PHRF+ORC boats will be bracketed based on ORC ratings
    - ORC boats will be bracketed on ORC ratings
    - PHRF boats will be bracketed on PHRF ratings
  - Race classes can be either (PHRF, ORC, Windseeker FS, Windseeker)

https://orc.org/race-managment/rms-files
All boats
https://data.orc.org/public/WPub.dll?action=DownRMS&CountryId=USA&ext=json&Family=1&VPPYear=2025

Specific boat
https://data.orc.org/public/WPub.dll?action=DownRMS&SailNo=59793&Family=ORC&ext=json
https://data.orc.org/public/WPub.dll?action=DownRMS&SailNo=59793&YachtName=Waymaker&ext=json

https://data.orc.org/public/WPub.dll?action=DownBoatRMS&RefNo=04560003WR9&ext=json

- Race config set ORC scoring type (upwind/downwind general purpose) or ()
- Add wind speed to RC and results editor
  - auto select

- Overall result optional
- Workflows
  - create new race
  - add boat
- RC image upload (for attaching paper race results)
- Integration with new CYCT website login
- Allow boat owners to upload images, ORC / PHRF cert
- Course chart (map)
- Race day feed - allow users to upload and share photos and pics
- Backup, export CSV/Excel function
- race can have list of RCs
- results show last edit date and user
  - websockets?
- Race check in page
  - page to add boats to race
  - websockets?
- Add timeseries https://github.com/chartjs/Chart.js
- Race editor and display table (show race duration)
- Show next race on home screen with countdown
- Show num boats in class
- Show num races in series 
- People show last race date
- Boats show last race date
- Deactivate vs delete
- Offline race upload (PWA / native app)
