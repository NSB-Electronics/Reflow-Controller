# Change Log
All notable changes to the Reflow Controller firmware will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/) 
and this project adheres to [Semantic Versioning](http://semver.org/).

## [2.4.0] - 2017-01-13
### Added
- Firmware command to serial and extra serial outs for GUI use

### Fixed
- Fix bug to show max time reached
- Fix bug when starting another cycle, PID not reset correctly leading to overshoot
- Fix bug when oven hot and on button pressed, starts auto when temp low

## [2.3.2] - 2016-11-25
### Fixed
- Fix startup to be able to set default mode as temp controller

## [2.3.1] - 2016-10-29
### Fixed
- Fixed nuisance temperature probe error
- Fixed issue in RFM library to detect radio

## [2.3.0] - 2016-10-07
### Added
- Add RF module code
- Add auto detect if RF module installed

## [2.2.0] - 2016-09-01
### Added
- Add changing Reflow temperature settings via serail and lcd
- Add check if reflow temp is not reached in certain time

### Fixed
- Fix button presses not recognised

## [2.1.0] - 2016-08-19
### Added
- Update settings

## [2.0.0] - 2016-08-19
### Added
- Combination of Reflow 1.30 and Temp 0.20
