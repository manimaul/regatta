//! The `regatta-contract` command line: `record`, `replay` and `diff` over the contract scenario.
//!
//! See `crates/contract/README.md` for the flags and the fresh-dump requirement.

use anyhow::Result;
use clap::{Parser, Subcommand};
use regatta_contract::{diff, record, replay, scenario, Options, ScenarioOptions};
use std::path::PathBuf;
use std::process::ExitCode;

#[derive(Parser)]
#[command(
    name = "regatta-contract",
    about = "Record, replay and diff the regatta REST API contract",
    version
)]
struct Cli {
    #[command(subcommand)]
    command: Command,
    /// No per-step progress on stderr.
    #[arg(long, global = true)]
    quiet: bool,
}

#[derive(Subcommand)]
enum Command {
    /// Run the scenario against a server on a fresh load of the golden dump and write one
    /// cassette per step.
    Record {
        #[arg(long, default_value = "http://localhost:8888")]
        base_url: String,
        #[arg(long, default_value = "crates/contract/cassettes")]
        out: PathBuf,
        /// The fixed clock the server runs with (REGATTA_FIXED_NOW); with it LoginResponse.expires is
        /// deterministic and compared, not volatile.
        #[arg(long)]
        fixed_now: Option<String>,
    },
    /// Run the scenario against a server on a fresh load of the golden dump and compare every
    /// response to its cassette; exit 1 on any mismatch.
    Replay {
        #[arg(long, default_value = "http://localhost:8889")]
        base_url: String,
        #[arg(long, default_value = "crates/contract/cassettes")]
        cassettes: PathBuf,
    },
    /// Run the scenario against two servers (both on fresh loads of the golden dump) and compare
    /// their responses to each other; exit 1 on any difference.
    Diff {
        /// A running server to diff against.
        #[arg(long, alias = "kotlin")]
        left: String,
        /// The server under test.
        #[arg(long, alias = "rust")]
        right: String,
        /// Append this many proptest-generated steps after the scenario.
        #[arg(long, default_value_t = 0)]
        random: usize,
        #[arg(long, default_value_t = 42)]
        seed: u64,
        /// Both servers run with this fixed clock: LoginResponse.expires is compared.
        #[arg(long)]
        fixed_now: Option<String>,
    },
}

#[tokio::main]
async fn main() -> Result<ExitCode> {
    let cli = Cli::parse();
    let quiet = cli.quiet;
    let report = match cli.command {
        Command::Record {
            base_url,
            out,
            fixed_now,
        } => {
            let scenario = scenario(&ScenarioOptions {
                expires_volatile: fixed_now.is_none(),
                ..ScenarioOptions::default()
            });
            let report = record(&base_url, &out, &scenario, &Options { quiet, fixed_now }).await?;
            eprintln!(
                "wrote {} cassettes to {}",
                report.outcomes.len(),
                out.display()
            );
            report
        }
        Command::Replay {
            base_url,
            cassettes,
        } => {
            let scenario = scenario(&ScenarioOptions::default());
            replay(
                &base_url,
                &cassettes,
                &scenario,
                &Options {
                    quiet,
                    fixed_now: None,
                },
            )
            .await?
        }
        Command::Diff {
            left,
            right,
            random,
            seed,
            fixed_now,
        } => {
            let scenario = scenario(&ScenarioOptions {
                expires_volatile: fixed_now.is_none(),
                random,
                seed,
            });
            diff(&left, &right, &scenario, &Options { quiet, fixed_now }).await?
        }
    };
    println!("{report}");
    Ok(if report.is_clean() {
        ExitCode::SUCCESS
    } else {
        ExitCode::from(1)
    })
}
