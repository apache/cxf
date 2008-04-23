#!/usr/bin/env perl


#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements. See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership. The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License. You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied. See the License for the
#  specific language governing permissions and limitations
#  under the License.



use strict;
use File::Find;
use XML::Parser;
use Getopt::Long;
use Cwd qw(abs_path getcwd);

my $master_version;
GetOptions('version=s' => \$master_version);

my $parser = new XML::Parser(Handlers => {Start => \&find_starts,
                                          End => \&find_ends,
                                          Char => \&find_text});

my ($top_ver_start_line, $parent_ver_start_line);
my ($top_ver_end_line, $parent_ver_end_line);
my ($project_seen, $profiles_seen, $profile_seen);
my ($id_seen, $id_rel_seen, $activation_seen);
my ($property_start_line, $property_end_line);
my ($activeByDefault_start_line, $activeByDefault_end_line);
my ($current_version, $get_version);

sub init_parse_state {
    $top_ver_start_line = $parent_ver_start_line = 0;
    $top_ver_end_line = $parent_ver_end_line = 0;
    $project_seen = $profiles_seen = $profile_seen = 0;
    $id_seen = $id_rel_seen = $activation_seen = 0;
    $property_start_line = $property_end_line = 0;
    $activeByDefault_start_line = $activeByDefault_end_line = 0;
    $current_version = $get_version = 0;
}

my $start_dir = shift;
if ($start_dir) {
    chdir $start_dir || die "$0: can't cd to \"$start_dir\": $!\n";
}
$start_dir = abs_path(getcwd());
File::Find::find(\&fixpomfiles, $start_dir);

my $parent_version;

sub fixpomfiles {
    return unless (/^pom.xml$/);
    my $pom = $_;
    my $curdir = abs_path(getcwd());
    return if ($curdir =~ m:/src/main/:);
    return if ($curdir =~ m:/target/classes/:);

    print "processing $File::Find::name\n";

    init_parse_state();
    $parser->parsefile($pom);
    return unless ($top_ver_start_line != 0 || $parent_ver_start_line != 0);
    my $rel_mode = undef;
    my ($rel_add, $rel_remove) = ('add', 'remove');
    local *POM;
    open(POM, $pom) || die "cannot open $pom: $!\n";
    my $line = 0;
    my $skip = 0;
    my @new;
    while (my $val = <POM>) {
        ++$line;
        next if ($line <= $skip);
        if ($line == $top_ver_start_line) {
            $val =~ m:^(\s*)<version>:;
            my $space = $1;
            my $newvers = $current_version;
            if ($current_version =~ /-SNAPSHOT$/) {
                $newvers =~ s/-SNAPSHOT$//;
                $rel_mode = $rel_add;
            } else {
                $newvers += 0.1;
                $newvers .= '-SNAPSHOT';
                $rel_mode = $rel_remove;
            }
            print "  existing version is $current_version\n";
            my $default_vers = $master_version || $newvers;
            print "  please enter new version [$default_vers]: ";
            my $version = <STDIN>;
            chomp $version;
            $version = $default_vers unless $version;
            push @new, "$space<version>$version</version>\n";
            $parent_version = $version if ($curdir eq $start_dir);
            $skip = $top_ver_end_line;
        } elsif ($line == $parent_ver_start_line) {
            $val =~ m:^(\s*)<version>:;
            push @new, "$1<version>$parent_version</version>\n";
            $skip = $parent_ver_end_line;
        } elsif ($line == $property_start_line && $rel_mode eq $rel_add) {
            $val =~ m:^(\s*)<property>:;
            push @new, "$1<activeByDefault>true</activeByDefault>\n";
            $skip = $property_end_line;
        } elsif ($line == $activeByDefault_start_line && $rel_mode eq $rel_remove) {
            $val =~ m:^(\s*)<activeByDefault>:;
            push @new, "$1<property>\n";
            push @new, "$1    <name>release</name>\n";
            push @new, "$1</property>\n";
            $skip = $activeByDefault_end_line;
        } else {
            push @new, $val;
        }
    }
    close POM;
    open(POM, ">${pom}") || die "cannot open $pom: $!\n";
    print POM @new;
    close POM;
}

sub find_starts {
    my ($p, $el) = @_;
    if ($el eq 'version') {
        my $parent = $p->current_element;
        if (defined($parent)) {
            $parent_ver_start_line = $p->current_line if ($parent eq 'parent');
            $top_ver_start_line = $p->current_line if ($parent eq 'project');
            $get_version = 1 if ($parent eq 'project');
        }
    } elsif ($el eq 'project') {
        $project_seen = 1;
    } elsif ($el eq 'profiles' && $project_seen) {
        $profiles_seen = 1;
    } elsif ($el eq 'profile' && $profiles_seen) {
        $profile_seen = 1;
    } elsif ($el eq 'id' && $profile_seen) {
        $id_seen = 1;
    } elsif ($el eq 'activation' && $id_rel_seen) {
        $activation_seen = 1;
    } elsif ($el eq 'property' && $activation_seen) {
        $property_start_line = $p->current_line;
    } elsif ($el eq 'activeByDefault' && $activation_seen) {
        $activeByDefault_start_line = $p->current_line;
    }
}

sub find_ends {
    my ($p, $el) = @_;
    if ($el eq 'version') {
        my $parent = $p->current_element;
        if (defined($parent)) {
            $parent_ver_end_line = $p->current_line if ($parent eq 'parent');
            $top_ver_end_line = $p->current_line if ($parent eq 'project');
            $get_version = 0;
        }
    } elsif ($el eq 'project') {
        $project_seen = 0;
    } elsif ($el eq 'profiles') {
        $profiles_seen = 0;
    } elsif ($el eq 'profile') {
        $profile_seen = 0;
    } elsif ($el eq 'id') {
        $id_seen = 0;
    } elsif ($el eq 'activation' && $id_rel_seen) {
        $id_rel_seen = 0;
        $activation_seen = 0;
    } elsif ($el eq 'property' && $activation_seen) {
        $property_end_line = $p->current_line;
    } elsif ($el eq 'activeByDefault' && $activation_seen) {
        $activeByDefault_end_line = $p->current_line;
    }
}

sub find_text {
    my ($p, $data) = @_;
    $id_rel_seen = ($data eq 'release') if $id_seen;
    $current_version = $data if ($get_version);
}
