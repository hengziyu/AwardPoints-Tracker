#!/bin/bash

echo "Cleaning up generated files..."

# Delete log file
rm -f app.log

# Delete generated Excel files
rm -f Awards_Summary.xlsx
rm -f Raw_Source.xlsx
rm -f Student_Awards.xlsx

# Delete database file
rm -f student.db

# Delete snapshot files
rm -f snapshot_*.json
rm -f snapshot_*.json.gz

# Delete target directory
rm -rf target

echo ""
echo "Cleanup complete. The project directory has been restored to its original state."

