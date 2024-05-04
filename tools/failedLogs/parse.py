import sys
import pandas as pd
import re

# Check if a command line argument was provided
if len(sys.argv) < 2:
    print("Usage: python parse.py <path_to_log_file>")
    sys.exit(1)  # Exit the script with an error code

# Use the first argument as the log file path
log_path = sys.argv[1]

# Function to read log file and handle multi-line entries
def read_logs(log_path):
    with open(log_path, 'r') as file:
        buffer = ""
        for line in file:
            # Check if the line starts a new log entry
            if ("Test Result" in line or "Summary for" in line) and buffer:
                yield buffer
                buffer = line  # Start a new buffer
            else:
                buffer += line  # Continue adding lines to the current buffer
        if buffer:
            yield buffer  # Yield the last log entry

# Define a function to parse each log entry
def parse_log_entry(entry):
    # Regex pattern to capture details, including the full 'result' string and optional 'stackTrace'
    pattern = r'(?P<entryType>Test Result|Summary for) (?P<requestId>.*?): TestResult\(requestId=.*, elapsedTime=(?P<elapsedTime>\d+), resultStatus=(?P<resultStatus>\w+), result=(?P<result>.*?)(, stackTrace=(?P<stackTrace>.*?))?\)$'
    match = re.search(pattern, entry, re.DOTALL)

    if match:
        # Determine the type of entry and adjust data dictionary accordingly
        data = match.groupdict()
        data['entryType'] = 'test' if 'Test Result' in data['entryType'] else 'summary'
        # Append stackTrace info if exists
        if data.get('stackTrace'):
            data['result'] += ', stackTrace=' + data['stackTrace']
        # Clean up data dictionary
        if 'stackTrace' in data:
            del data['stackTrace']
        return data['entryType'], {key: val for key, val in data.items() if key not in ['entryType']}
    return None, None

# Prepare lists for storing parsed data
test_results = []
summaries = []

# Read and parse the log entries
for entry in read_logs(log_path):
    entry_type, data = parse_log_entry(entry)
    if data:
        if entry_type == 'test':
            test_results.append(data)
        elif entry_type == 'summary':
            summaries.append(data)

# Convert the parsed data into DataFrames
df_test = pd.DataFrame(test_results)
df_summary = pd.DataFrame(summaries)

# Rename columns for the test results CSV
df_test.rename(columns={
    'requestId': 'REQUEST_ID',
    'elapsedTime': 'ELAPSED_TIME',
    'resultStatus': 'RESULT_STATUS',
    'result': 'RESULT'
}, inplace=True)

# Rename columns for the summaries CSV
df_summary.rename(columns={
    'requestId': 'REQUEST_ID',
    'elapsedTime': 'ELAPSED_TIME',
    'resultStatus': 'RESULT_STATUS',
    'result': 'RESULT'
}, inplace=True)

# Export the DataFrames to separate CSV files
df_test.to_csv('test_results.csv', index=False)
df_summary.to_csv('summaries.csv', index=False)

print("Test results and summaries have been written to separate CSV files with updated header names.")
