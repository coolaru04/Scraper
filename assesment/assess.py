"""
INTERNAL DO NOT MODIFY
"""

import json
import os, shutil, re
import sys
import traceback
import xml.etree.ElementTree as ET

"""
Copy kwargs from wherever they are in the user project to ./assets/ dir
kwargs can be 'chrome_logs', 'testng_report', and 'checkstyle_report'
"""
def copy_and_arrange(**kwargs):
    assets_dir = os.path.join('assesment', 'assets')
    if not os.path.exists(assets_dir):
        os.makedirs(assets_dir)

    for key, src_path in kwargs.items():
        if os.path.exists(src_path):
            dest_path = os.path.join(assets_dir, os.path.basename(src_path))
            shutil.copy(src_path, dest_path)
        else:
            print(f"Source file {src_path} not found for {key}.")

"""
Reads kwargs['checkstyle_report'] and creates a JSON report.
"""
def checkstyle_assess(**kwargs):
    report = {}
    hints = []
    
    try:
        with open(kwargs['checkstyle_report'], 'r') as file:
            content = file.read()
    except Exception as e:
        print(f"Error reading file: {e}")
        return {}, []  # Ensuring two values are always returned

    instructions = kwargs.get('instructions', [])
    for inst in instructions:
        if not inst['is_enabled']:
            report[inst['out'].format('NA', inst['min_limit'], inst['max_limit'])] = 'TEST_STATUS_SKIPPED'
            continue

        occurrences = len(re.findall(inst['indicator'], content))
        min_limit = inst.get('min_limit', None)
        max_limit = inst.get('max_limit', None)
        
        if min_limit is not None and max_limit is not None:
            test_result = min_limit <= occurrences <= max_limit
        elif min_limit is not None:
            test_result = occurrences >= min_limit
        elif max_limit is not None:
            test_result = occurrences <= max_limit
        else:
            test_result = True

        if test_result:
            report[inst['out'].format(occurrences, min_limit or 'No limit', max_limit or 'No limit')] = 'TEST_STATUS_SUCCESS'
        else:
            report[inst['out'].format(occurrences, min_limit or 'No limit', max_limit or 'No limit')] = 'TEST_STATUS_FAILURE'
            hints.append(inst['out'].format(occurrences, min_limit or 'No limit', max_limit or 'No limit').split('.')[1])

    return report, hints  # Always return two values

"""
Taken from generatefilteredlogs (previously written script)
"""
def construct_log_json(log_text):
    LOG = {"actions": []}
    log_frame = ""
    log_object_frame = {"COMMAND": None, "RESPONSE": None}
    selector = None

    with open(log_text, 'r', encoding='utf-8') as file:
        lines = [line.strip() for line in file.readlines()]

    for line in lines:
        if '[INFO]' in line and 'COMMAND' in line:
            if "COMMAND ExecuteScript" not in str(log_object_frame):
                LOG["actions"].append(log_object_frame)
            selector = "COMMAND"
            log_object_frame = {"COMMAND": None, "RESPONSE": None}
            log_frame = ""
        elif '[INFO]' in line and 'RESPONSE' in line:
            selector = "RESPONSE"

        if selector:
            if line.startswith('[') and log_frame == "":
                log_frame += line
                log_object_frame[selector] = log_frame
            elif line.startswith('[') and log_frame != "":
                log_frame = ""
                selector = None
            else:
                log_frame += line
                log_object_frame[selector] = log_frame

    json.dump(LOG, open(os.path.join('assesment', 'assets', 'filtered_logs.json'), 'w+'), indent=4)
    return LOG

"""
Reads report -> kwargs['testng_report_path'] and creates a JSON report.
"""
def testng_validation_assess(**kwargs):
    testng_report_path = kwargs.get('testng_report_path')
    testng_assessment = kwargs.get('testng_assessment')
    filtered_log_path = kwargs.get('filtered_log')
    response = {}
    hints_all = []

    try:
        tree = ET.parse(testng_report_path)
        root = tree.getroot()
        for test in testng_assessment:
            if not test['is_enabled']:
                response[test['test_case_fe']] = "TEST_STATUS_SKIPPED"
                continue

            tree = root.findall(".//test-method[@name='%s']" % test['testng_test_name'])
            if not tree:
                result = {f"{test['test_case_fe']}.TestNG Test Method '{test['testng_test_name']}' is not Implemented": "TEST_STATUS_FAILURE"}
                hints = [f"Implement TestNG Test Method '{test['testng_test_name']}' for assessment to register it!"]
                response.update(result)
                hints_all += hints
                continue

            for test_method in tree:
                status = test_method.get('status')
                if status == 'PASS':
                    result, hints = chromelog_validation_assess(
                        log_path=filtered_log_path,
                        chrome_log_assessment=test['chrome_log_assessment'],
                        test_suite=test['test_case_fe']
                    )
                elif status == 'FAIL':
                    response[test['test_case_fe']] = "TEST_STATUS_FAILURE"
                
                response.update(result)
                hints_all += hints

        return response, hints_all
    except Exception as e:
        print(f"Internal error, check instructions or connect with support - {str(e)}")
        return {}, []  # Ensuring safe return

# Utility function
def safe_string_fetch(val):
    return val if val is not None else ""

# Main execution
if __name__ == "__main__":
    try:
        if len(sys.argv) < 5:
            print("Oops! Do not modify the run assessment file!")
            sys.exit(1)

        checkstyle_file = sys.argv[1]
        chromelog_file = sys.argv[2]
        testngreport_file = sys.argv[3]

        copy_and_arrange(
            checkstyle_file=checkstyle_file,
            chromelog_file=chromelog_file,
            testngreport_file=testngreport_file
        )

        checkstyle_path = os.path.join(os.getcwd(), 'assesment', 'assets', os.path.basename(checkstyle_file))
        chromelog_path = os.path.join(os.getcwd(), 'assesment', 'assets', os.path.basename(chromelog_file))
        testngreport_path = os.path.join(os.getcwd(), 'assesment', 'assets', os.path.basename(testngreport_file))

        with open(sys.argv[4], 'r') as file:
            assessment_instructions = json.load(file)

        result = checkstyle_assess(checkstyle_report=checkstyle_path, instructions=assessment_instructions.get('quality_eval', []))
        if isinstance(result, tuple) and len(result) == 2:
            checkstyle_result, hints_checkstyle = result
        else:
            checkstyle_result, hints_checkstyle = {}, []

        construct_log_json(chromelog_path)
        log_path = os.path.join(os.getcwd(), 'assesment', 'assets', 'filtered_logs.json')

        result, hints = testng_validation_assess(
            testng_report_path=testngreport_path,
            filtered_log=log_path,
            testng_assessment=assessment_instructions.get('instruction_set', [])
        )

        final_result = {**checkstyle_result, **result}
        hints += hints_checkstyle

        json.dump(final_result, open("assesment_result.json", 'w+'), indent=4)
        print("\033[33m\nAssessment Successful.\033[0m")

        if hints:
            print("\033[91m\nHere are some violations to check in automation:\033[0m")
            for idx, hint in enumerate(hints, start=1):
                print(f"\t{idx} - {hint}")
        else:
            print("\033[32m\nAll Checks Passed! Please commit and push code.\033[0m")

    except Exception as e:
        print(f"Critical error: {str(e)}")
        sys.exit(1)
