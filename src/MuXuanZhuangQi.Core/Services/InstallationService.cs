public class InstallationService : IInstallationService
{
    private readonly Dictionary<InstallationType, IInstallationStrategy> _strategies;

    public InstallationService()
    {
        _strategies = new Dictionary<InstallationType, IInstallationStrategy>
        {
            { InstallationType.System, new SystemInstallationStrategy() },
            { InstallationType.Shizuku, new ShizukuInstallationStrategy() },
            { InstallationType.Root, new RootInstallationStrategy() }
        };
    }

    public async Task<bool> InstallAsync(SoftwarePackage package, IProgress<int> progress)
    {
        if (!_strategies.TryGetValue(package.SelectedInstallationType, out var strategy))
        {
            throw new InvalidOperationException("不支持的安装方式");
        }

        if (!strategy.IsSupported(package))
        {
            throw new InvalidOperationException("该软件包不支持选择的安装方式");
        }

        return await strategy.Install(package, progress);
    }

    // ... existing code ...
}