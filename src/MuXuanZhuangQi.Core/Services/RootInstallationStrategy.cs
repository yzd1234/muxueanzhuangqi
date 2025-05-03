public class RootInstallationStrategy : IInstallationStrategy
{
    public async Task<bool> Install(SoftwarePackage package, IProgress<int> progress)
    {
        // 实现Root安装逻辑
        return await Task.FromResult(true);
    }

    public async Task<bool> Uninstall(string packageId)
    {
        // 实现Root卸载逻辑
        return await Task.FromResult(true);
    }

    public bool IsSupported(SoftwarePackage package)
    {
        return package.SupportedInstallationTypes.Contains(InstallationType.Root);
    }
}