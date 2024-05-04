# Parsing Test Logs

If a test fails to write to a CSV, use this to parse the log file into a CSV.

## Step 1: Install Poetry
If you haven't already installed Poetry, you can do so by using the following command in your terminal. This installation script works on Unix-based systems (Linux, macOS) and Windows PowerShell:

```bash
curl -sSL https://install.python-poetry.org | python3 -
```

Alternatively, if you prefer to use pip:

```bash
pip install poetry
```

## Step 2: Initialize Your Project
Navigate to your project directory, or create a new one if necessary, and then run:

```bash
poetry init
```

This command will guide you through creating a new pyproject.toml file. You can either specify dependencies and other settings during this process or modify the pyproject.toml manually afterwards.

## Step 3: Configure Poetry for Virtual Environment Management
Poetry can be configured to create a virtual environment either inside your project directory or in a centralized location managed by Poetry. To set Poetry to create the virtual environment within your project's directory, run:

```bash
poetry config virtualenvs.in-project true
```
This setting ensures that the virtual environment is locally accessible within your project folder, making it easier to manage project-specific dependencies.

## Step 4: Add Pandas as a Dependency
To add Pandas to your project, use the following command:

```bash
poetry add pandas
```

This command will automatically find, resolve, and install the latest version of Pandas, along with its dependencies, and update your pyproject.toml and poetry.lock files accordingly.

## Step 5: Install Dependencies
If you've manually added Pandas to your pyproject.toml without using poetry add, or if there are other dependencies listed, you can install all dependencies specified in your pyproject.toml by running:

```bash
poetry install
```

This will create the virtual environment if it doesn't already exist, and install all specified dependencies.

## Step 6: Using Your Virtual Environment
With Poetry, you do not need to manually activate the virtual environment. You can run Python scripts or open a Python shell using Poetry, which will automatically use the correct environment:

```bash
poetry run python your_script.py
```

Or to open a shell:

```bash
poetry shell
```

This will spawn a shell with the virtual environment activated.

