# Building guide

To easily build the e-voting system, we provide a Docker image available
on [GitLab](https://gitlab.com/swisspost-evoting/e-voting/evoting-e2e-dev/container_registry/6352791). This is the recommended way to build the
e-voting system since it requires no installation nor configuration on the user machine, except having to install Docker.

## Prerequisites

- **Windows OS**

  The build process has been specifically tested and approved on the Windows operating system (Windows 10 and Windows 11) running the provided Docker
  image. We therefore support only Windows.

- **x86 Architecture**

  The build process is specifically tailored for an x86 architecture. This is due to unique aspects of the Frontend modules loaded from Node, which
  may not function correctly under other architectures such as ARM.

- **Have a Unix shell**

  To easily configure the e-voting environment, various Linux commands are useful. Therefore, you need to have a Unix shell available on the
  Windows machine. You can, for example, use the [portable Git-bash](https://git-scm.com/download/win).

- **Install Docker Desktop on Windows**

  Please refer to the official [Get Docker](https://docs.docker.com/desktop/install/windows-install/) guide. Make sure to download the latest
  available version.

- **GitLab connectivity**

  The provided Docker image and e-voting system are published on GitLab.

- **Memory**

  The result of the build process is a tarball containing the full e-voting system which requires a significant amount of memory. Please ensure the
  shared folder (`C:/tmp`) has at least 15GB of free space.

- **Optional: Availability of a published compatible end-to-end**

  In case you want to perform an end-to-end test with the resulting built e-voting system, please check if the corresponding version
  of [end-to-end](https://gitlab.com/swisspost-evoting/e-voting/evoting-e2e-dev/-/blob/master/README.md#e-voting-supported-versions) exists.

## Pull the Docker image

Run the command

```sh
docker pull registry.gitlab.com/swisspost-evoting/e-voting/evoting-e2e-dev/e-voting/evoting-build:<VERSION>
```

For example, if you want to build e-voting 1.5.2.2, you need to run

```sh
docker pull registry.gitlab.com/swisspost-evoting/e-voting/evoting-e2e-dev/e-voting/evoting-build:1.5.2.2
```

Moreover, you can also build the Docker image yourself using
the [build Docker file](https://gitlab.com/swisspost-evoting/e-voting/evoting-e2e-dev/-/blob/master/build-image/evoting-build.Dockerfile).

## Run the Docker image in a container

Run the command

```sh
docker run -v <SHARED_VOLUME>:/home/baseuser/data -it registry.gitlab.com/swisspost-evoting/e-voting/evoting-e2e-dev/e-voting/evoting-build:<VERSION>
```

When running the Docker image, you need to specify a shared volume between your machine and the Docker container. **This shared volume must be empty**.

For example, if you want to share `C:/tmp` and use the evoting-build Docker image 1.5.2.2 on a Windows machine, you need to run

```sh
winpty docker run -v c:\\tmp:/home/baseuser/data -it registry.gitlab.com/swisspost-evoting/e-voting/evoting-e2e-dev/e-voting/evoting-build:1.5.2.2
```

If you select the “Use MinTTY” option when you have installed your bash, your Bash prompt will be hosted in the MinTTY terminal emulator, rather than
the CMD console that ships with Windows. The MinTTY terminal emulator is not compatible with Windows console programs unless you prefix your commands
with winpty.

As we build on linux and our artifacts are created with Windows we need 'Wine'.
It is automatically downloaded in the 'evoting-build' docker image.

## Run the e-voting build in the Docker container

After having successfully ran the Docker image, you are connected to the bash of the Docker container. Now, in the container's bash, you need to run
the build script:

```sh
./build.sh -ev <E-VOTING_VERSION> -vv <VERIFIER_VERSION> -dv <DATA-INTEGRATION-SERVICE_VERSION>
```

For example, if you want to build the e-voting system 1.5.2.2, the verifier 1.6.2.1 and the data-integration-service 2.9.2.2 you need to run:

```sh
./build.sh -ev 1.5.2.2 -vv 1.6.2.1 -dv 2.9.2.2
```

If you do not provide the versions of the e-voting system, the verifier and/or the data-integration-service, the build script will use the latest
master version of each missing repository version.

The build script ensures that the correct build dependencies are provided using
the [environment-check script](https://gitlab.com/swisspost-evoting/e-voting/evoting-e2e-dev/-/blob/master/build-image/resources/environment-checker.sh).

The build process will take some time, depending on the versions you want to build and the performance of your machine, but you can expect around 25
minutes.

Once the process of building is achieved, you can exit the container with the `exit` command.

In the shared volume of your machine (for example `C:/tmp`), there will be

- a `build.tar.gz` archive containing the built e-voting system
- an `prepare-e2e.sh` script that you can use to configure and deploy locally all services needed to run an e-voting end-to-end test.

## Remarks

- For detailed instructions on how to use the e2e.sh script, please refer to
  the [README.md](https://gitlab.com/swisspost-evoting/e-voting/evoting-e2e-dev/-/blob/master/README.md) of the end-to-end repository.
- Our build is fully [reproducible](https://reproducible-builds.org/), allowing researchers to verify the path from source code to binaries.
