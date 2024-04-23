#!/usr/bin/perl
use strict;
use warnings;
use JSON;
use Digest::MD5 qw(md5_hex);

# Check for the correct number of arguments
unless (@ARGV == 2) {
    die "Usage: $0 <input_file> <output_directory>\n";
}

my $filename = $ARGV[0];
open(my $fh, '<', $filename) or die "Could not open file '$filename' $!";

my $json = JSON->new->allow_nonref;

# Read output directory from command line argument and ensure it ends with a slash
my $output_directory = $ARGV[1];
$output_directory .= '/' unless $output_directory =~ /\/$/;

# Hash to keep track of duplicate request_id counts and written theories
my %request_id_count;
my %written_theories;

my $current_request_id = '';
my $collecting = 0;
my $data = '';

while (my $line = <$fh>) {
    chomp $line;
    if ($line =~ /.*:\d+:optimization\s+([^:]+): "(.*)$/) {
        if ($collecting) {
            # Save the previous record to its own file if it's unique
            save_to_file($current_request_id, $data);
            $data = '';
        }
        # Start a new record
        $current_request_id = $1;
        $data = $2;
        $collecting = 1;  # Start collecting lines
    } elsif ($collecting) {
        # Continue collecting lines if they do not start a new record
        if ($line =~ /^.*:\d+:optimization/) {
            # If a new record starts, save the previous if unique and reset
            save_to_file($current_request_id, $data);
            $data = '';
            $collecting = 0;
            redo;  # Re-evaluate this line because it's a new record start
        } else {
            $data .= "\n" . $line;
        }
    }
}

# Save the last record if it's unique
if ($collecting) {
    save_to_file($current_request_id, $data);
}

close $fh;

sub save_to_file {
    my ($request_id, $theory) = @_;
    
    # Remove everything from the last escaped quote to the end of the string
    $theory =~ s/\".*$//s;  # The 's' modifier allows '.' to match newline characters
    $theory =~ s/^\s+|\s+$//g;
    
    my $theory_hash = md5_hex($theory);
    # Check if this theory has already been written
    return if $written_theories{$theory_hash};

    # Mark this theory as written
    $written_theories{$theory_hash} = 1;

    # Increment the count for the current request_id or initialize it
    $request_id_count{$request_id}++;
    # Prepend the count to the request_id for the filename
    my $unique_request_id = $request_id . "_" . $request_id_count{$request_id};
    my $output_file = $output_directory . $unique_request_id . ".json";
    $output_file =~ s/\s+/_/g; # Replace spaces with underscores for filenames

    # Ensure the output directory exists or create it
    unless (-d $output_directory) {
        mkdir $output_directory or die "Unable to create directory '$output_directory': $!";
    }

    open(my $out_fh, '>', $output_file) or die "Could not open file '$output_file' $!";
    print $out_fh $json->encode({ request_id => $unique_request_id, theory => $theory });
    close $out_fh;
}
