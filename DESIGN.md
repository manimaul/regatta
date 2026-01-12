# Boat (table)
Table of boats (name, sail no, type, current rating info, associated boat)

# Person (table)
Table of skippers (first, last, is club member)

# ORC (table) **new
Table containing ORC certificate rating information for a particular boat.

# Race (table)
Table of races (name, optional series, PHRF correction factor)

** need to add ORC scoring option...but this may need to be extensible to be granular (per RaceClass)

# Bracket (table)
Table of brackets (name, description, race class, min rating, max rating)

# RaceResults (table)
Table containing a boat's finish time, rating (at time of race), codes and penalties. 

# Registration (table) **new
Table of boats registered for a specific race (boat id, race id, race time id)

** can a boat register for multiple brackets? if the brackets have the same start time
... perhaps this is only available for orc_phrf

# RaceClass (table)
Table of race class names and associated rating type. 
A Race record should have at least one associated RaceClass. 
A race class represents a larger group of yachts (at least 1 Bracket) with a single start time and course. 
A race class is associated with multiple Races.

# RaceTime (table)
Table containing race start and end date-times per RaceClass (start, end, race class id, race id) .

# Series (table)
Table containing race series (id, name, is active, sort order) . A series can have many associated Race records. 

----------------------------

## Race Registration

Logged in users can register racers for individual, series or all races. Later, the CYCT race registration will call
a webhook for this. RC can add also add a racer on checkin page. Registered racers are automatically checked in.

