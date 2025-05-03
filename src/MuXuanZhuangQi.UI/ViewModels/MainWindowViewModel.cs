public class MainWindowViewModel : ViewModelBase
{
    private InstallationType _selectedInstallationType;
    
    public InstallationType SelectedInstallationType
    {
        get => _selectedInstallationType;
        set
        {
            if (SetProperty(ref _selectedInstallationType, value))
            {
                if (SelectedPackage != null)
                {
                    SelectedPackage.SelectedInstallationType = value;
                }
                InstallCommand.RaiseCanExecuteChanged();
            }
        }
    }

    private bool CanExecuteInstall()
    {
        return SelectedPackage != null && 
               SelectedPackage.SupportedInstallationTypes.Contains(SelectedInstallationType);
    }

    private readonly IInstallationService _installationService;
    private ObservableCollection<SoftwarePackage> _availablePackages;
    private int _installationProgress;
    
    public MainWindowViewModel(IInstallationService installationService)
    {
        _installationService = installationService;
        InstallCommand = new RelayCommand(ExecuteInstall, CanExecuteInstall);
        UninstallCommand = new RelayCommand(ExecuteUninstall, CanExecuteUninstall);
    }

    public ICommand InstallCommand { get; }
    public ICommand UninstallCommand { get; }

    public ObservableCollection<SoftwarePackage> AvailablePackages
    {
        get => _availablePackages;
        set => SetProperty(ref _availablePackages, value);
    }

    public int InstallationProgress
    {
        get => _installationProgress;
        set => SetProperty(ref _installationProgress, value);
    }

    // ... 其他命令和属性实现
}